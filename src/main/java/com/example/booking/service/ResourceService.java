package com.example.booking.service;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resources;

    public ResourceService(ResourceRepository resources) { this.resources = resources; }

    public List<ResourceResponse> findAll() { return resources.findAll().stream().map(this::response).toList(); }

    public ResourceResponse findById(Long id) { return response(resource(id)); }

    public ResourceResponse create(ResourceRequest request) {
        return response(resources.save(apply(new Resource(), request)));
    }

    public ResourceResponse update(Long id, ResourceRequest request) {
        return response(resources.save(apply(resource(id), request)));
    }

    public void delete(Long id) { resources.delete(resource(id)); }

    public Resource resource(Long id) {
        return resources.findById(id).orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }

    private Resource apply(Resource resource, ResourceRequest request) {
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setPrice(request.getPrice());
        resource.setAvailable(request.getAvailable() == null || request.getAvailable());
        return resource;
    }

    private ResourceResponse response(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setName(resource.getName());
        response.setDescription(resource.getDescription());
        response.setType(resource.getType());
        response.setPrice(resource.getPrice());
        response.setAvailable(resource.isAvailable());
        return response;
    }
}