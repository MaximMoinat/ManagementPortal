package org.radarcns.management.service.mapper;

import org.radarcns.management.domain.*;
import org.radarcns.management.service.dto.DescriptiveDeviceDTO;
import org.radarcns.management.service.dto.DeviceDTO;

import org.mapstruct.*;
import java.util.List;
import org.radarcns.management.service.mapper.decorator.DeviceMapperDecorator;

/**
 * Mapper for the entity Device and its DTO DeviceDTO.
 */
@Mapper(componentModel = "spring", uses = {DeviceTypeMapper.class, ProjectMapper.class})
@DecoratedWith(DeviceMapperDecorator.class)
public interface DeviceMapper {

    @Mapping(source = "deviceType.id", target = "deviceTypeId")
    @Mapping(source = "project.id", target = "projectId")
    DeviceDTO deviceToDeviceDTO(Device device);

//    @Mapping(target = "deviceTypeAndPhysicalId" , ignore = true)
    DescriptiveDeviceDTO deviceToDescriptiveDeviceDTO(Device device);

    List<DescriptiveDeviceDTO> devicesToDescriptiveDeviceDTOs(List<Device> devices);

    Device descriptiveDTOToDevice(DescriptiveDeviceDTO descriptiveDeviceDTO);

    List<Device> descriptiveDTOsToDevices(List<DescriptiveDeviceDTO> descriptiveDeviceDTOS);

    List<DeviceDTO> devicesToDeviceDTOs(List<Device> devices);

    @Mapping(source = "deviceTypeId", target = "deviceType")
    @Mapping(source = "projectId", target = "project")
    @Mapping(target = "patients" , ignore = false)
    Device deviceDTOToDevice(DeviceDTO deviceDTO);

    List<Device> deviceDTOsToDevices(List<DeviceDTO> deviceDTOs);
    /**
     * generating the fromId for all mappers if the databaseType is sql, as the class has relationship to it might need it, instead of
     * creating a new attribute to know if the entity has any relationship from some other entity
     *
     * @param id id of the entity
     * @return the entity instance
     */

    default Device deviceFromId(Long id) {
        if (id == null) {
            return null;
        }
        Device device = new Device();
        device.setId(id);
        return device;
    }


}
