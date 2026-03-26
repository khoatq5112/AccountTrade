package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when credentials are assigned to a buyer.
 */
@Getter
public class CredentialAssignedEvent extends NotificationEvent {

    private final CredentialAssignment assignment;

    public CredentialAssignedEvent(Object source, CredentialAssignment assignment, User recipient) {
        super(source, "CREDENTIAL_ASSIGNED", recipient, buildPayload(assignment));
        this.assignment = assignment;
    }

    private static Map<String, Object> buildPayload(CredentialAssignment assignment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("assignmentId", assignment.getAssignmentId());
        payload.put("assignmentStatus", assignment.getAssignmentStatus());
        
        if (assignment.getOrderItem() != null) {
            payload.put("orderItemId", assignment.getOrderItem().getOrderItemId());
            if (assignment.getOrderItem().getOrder() != null) {
                payload.put("orderId", assignment.getOrderItem().getOrder().getOrderId());
                payload.put("orderNumber", assignment.getOrderItem().getOrder().getOrderNumber());
            }
        }
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "CREDENTIAL";
    }

    @Override
    public int getDefaultPriority() {
        return 3; // HIGH - credentials are important
    }

    @Override
    public String getActionUrl() {
        if (assignment.getOrderItem() != null && assignment.getOrderItem().getOrder() != null) {
            return "/orders/" + assignment.getOrderItem().getOrder().getOrderId();
        }
        return "/orders";
    }

    @Override
    public String getRelatedEntityType() {
        return "CREDENTIAL_ASSIGNMENT";
    }

    @Override
    public Long getRelatedEntityId() {
        return assignment.getAssignmentId();
    }
}
