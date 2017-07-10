package org.radarcns.management.service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.radarcns.management.domain.Authority;
import org.radarcns.management.domain.Source;
import org.radarcns.management.domain.Subject;
import org.radarcns.management.domain.Role;
import org.radarcns.management.domain.User;
import org.radarcns.management.repository.AuthorityRepository;
import org.radarcns.management.repository.SourceRepository;
import org.radarcns.management.repository.SubjectRepository;
import org.radarcns.management.repository.RoleRepository;
import org.radarcns.management.security.AuthoritiesConstants;
import org.radarcns.management.service.dto.SubjectDTO;
import org.radarcns.management.service.mapper.SubjectMapper;
import org.radarcns.management.service.mapper.ProjectMapper;
import org.radarcns.management.service.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by nivethika on 26-5-17.
 */
@Service
@Transactional
public class SubjectService {

    private final Logger log = LoggerFactory.getLogger(SubjectService.class);

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;


    @Transactional
    public SubjectDTO createSubject(SubjectDTO subjectDTO) throws IllegalAccessException {
        User currentUser = userService.getUserWithAuthorities();
        Subject subject = subjectMapper.subjectDTOToSubject(subjectDTO);
        List<String> currentUserAuthorities = currentUser.getAuthorities().stream()
            .map(Authority::getName).collect(
                Collectors.toList());
        if (currentUserAuthorities.contains(AuthoritiesConstants.PROJECT_ADMIN)
            && !currentUserAuthorities.contains(AuthoritiesConstants.SYS_ADMIN) && !currentUser.getProject().equals(
            subject.getUser().getProject())) {
            log.debug("Validate project admin");
            throw new IllegalAccessException("This project-admin is not allowed to create Subjects under this project");
        }


        User user = subject.getUser();
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository
            .findOneByAuthorityNameAndProjectId(AuthoritiesConstants.PARTICIPANT,
                subject.getUser().getProject().getId());
        if (role != null) {
            roles.add(role);
        } else {
            Role subjectrole = new Role();
            subjectrole.setAuthority(
                authorityRepository.findByAuthorityName(AuthoritiesConstants.PARTICIPANT));
            subjectrole.setProject(subject.getUser().getProject());
            roleRepository.save(subjectrole);
            roles.add(subjectrole);
        }
        user.setRoles(roles);
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setLangKey(
            "en"); // setting default language key to "en", required to set email context,
        // Find a workaround
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(ZonedDateTime.now());
        user.setActivated(false);
        subject.setUser(user);
        subject = subjectRepository.save(subject);
        if (subject.getId() != null) {
            mailService.sendCreationEmailForGivenEmail(subject.getUser(), subjectDTO.getEmail());
        }
        //set if any devices are set as assigned
        if(subject.getSources() !=null && !subject.getSources().isEmpty()) {
            for (Source source : subject.getSources()) {
                source.setAssigned(true);
            }
        }
        return subjectMapper.subjectToSubjectDTO(subject);
    }

    @Transactional
    public SubjectDTO updateSubject(SubjectDTO subjectDTO) throws IllegalAccessException {
        if (subjectDTO.getId() == null) {
            return createSubject(subjectDTO);
        }
        //  TODO : add security and owner check for the resource
        Subject subject = subjectRepository.findOne(subjectDTO.getId());
        //reset all the sources assigned to a subject to unassigned
        for(Source source : subject.getSources()) {
            source.setAssigned(false);
            sourceRepository.save(source);
        }
        //set only the devices assigned to a subject as assigned
        subjectMapper.safeUpdateSubjectFromDTO(subjectDTO, subject);
        for(Source source : subject.getSources()) {
            source.setAssigned(true);
        }
        subject.getUser().setProject(projectMapper.projectDTOToProject(subjectDTO.getProject()));
        subject = subjectRepository.save(subject);

        return subjectMapper.subjectToSubjectDTO(subject);
    }


    public List<SubjectDTO> findAll() {

        List<Subject> subjects = new LinkedList<>();
//        List result = new LinkedList();
        User currentUser = userService.getUserWithAuthorities();
        List<String> currentUserAuthorities = currentUser.getAuthorities().stream().map(Authority::getName).collect(
            Collectors.toList());
        if(currentUserAuthorities.contains(AuthoritiesConstants.SYS_ADMIN)) {
            log.debug("Request to get all subjects");
            subjects = subjectRepository.findAllWithEagerRelationships();/*.stream()
                .map(projectMapper::projectToProjectDTO)
                .collect(Collectors.toCollection(LinkedList::new));*/
        }
        else if(currentUserAuthorities.contains(AuthoritiesConstants.PROJECT_ADMIN)) {
            log.debug("Request to get Sources of admin's project ");
            subjects = subjectRepository.findAllByProjectId(currentUser.getProject().getId());
        }
        return subjectMapper.subjectsToSubjectDTOs(subjects);
    }
}