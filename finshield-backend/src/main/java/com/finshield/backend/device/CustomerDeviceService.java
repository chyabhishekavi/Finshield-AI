package com.finshield.backend.device;

import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.repository.CustomerRepository;
import com.finshield.backend.device.api.AddCustomerDeviceRequest;
import com.finshield.backend.device.api.CustomerDeviceResponse;
import com.finshield.backend.device.api.UpdateCustomerDeviceRequest;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.device.repository.CustomerDeviceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerDeviceService {

    private final CustomerDeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;

    public CustomerDeviceService(
            CustomerDeviceRepository deviceRepository,
            CustomerRepository customerRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerDeviceResponse add(UUID customerId, AddCustomerDeviceRequest request) {
        Customer customer = findCustomer(customerId);
        String deviceId = request.deviceId().trim();
        if (deviceRepository.existsByCustomerIdAndDeviceId(customerId, deviceId)) {
            throw new BadRequestException("This device is already registered for the customer");
        }

        CustomerDevice device = new CustomerDevice(
                customer,
                deviceId,
                request.deviceType(),
                request.ipAddress(),
                request.location(),
                Boolean.TRUE.equals(request.trusted()),
                Instant.now()
        );

        try {
            return CustomerDeviceResponse.from(deviceRepository.saveAndFlush(device));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("This device is already registered for the customer", exception);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDeviceResponse> list(UUID customerId, int page, int size) {
        findCustomer(customerId);
        return PageResponse.from(
                deviceRepository.findAllByCustomerId(
                        customerId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastSeenAt"))
                ),
                CustomerDeviceResponse::from
        );
    }

    @Transactional
    public CustomerDeviceResponse update(
            UUID customerId,
            UUID deviceRecordId,
            UpdateCustomerDeviceRequest request
    ) {
        CustomerDevice device = deviceRepository.findByIdAndCustomerId(deviceRecordId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer device", deviceRecordId));

        if (request.deviceType() == null
                && request.ipAddress() == null
                && request.location() == null
                && request.trusted() == null) {
            throw new BadRequestException("At least one device field must be provided");
        }

        device.updateObservation(
                request.deviceType() == null ? device.getDeviceType() : request.deviceType(),
                request.ipAddress() == null ? device.getIpAddress() : request.ipAddress(),
                request.location() == null ? device.getLocation() : request.location(),
                request.trusted() == null ? device.isTrusted() : request.trusted(),
                Instant.now()
        );
        return CustomerDeviceResponse.from(deviceRepository.saveAndFlush(device));
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }
}
