package com.vasav.springtalentengine.billing;

import com.vasav.springtalentengine.tenant.TenantCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FreePlanStarter {

    private final Subscriptions subscriptions;

    @EventListener
    public void on(TenantCreated event) {
        subscriptions.start(event.tenantId());
    }
}
