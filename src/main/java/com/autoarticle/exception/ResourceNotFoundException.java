package com.autoarticle.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(buildMessage(resource, String.valueOf(id)));
    }

    public ResourceNotFoundException(String resource, String id) {
        super(buildMessage(resource, id));
    }

    private static String buildMessage(String resource, String id) {
        if (id == null || id.isBlank()) {
            return resource + " 不存在";
        }
        return resource + " 不存在，ID: " + id;
    }
}
