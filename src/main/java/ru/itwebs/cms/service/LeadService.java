package ru.itwebs.cms.service;

import org.springframework.stereotype.Service;
import ru.itwebs.cms.dto.LeadRequest;
import ru.itwebs.cms.entity.Lead;
import ru.itwebs.cms.repository.LeadRepository;
import java.util.List;

@Service
public class LeadService {
    private final LeadRepository leads;
    private final AmoCrmIntegrationService amoCrm;
    public LeadService(LeadRepository leads, AmoCrmIntegrationService amoCrm) {
        this.leads = leads;
        this.amoCrm = amoCrm;
    }
    public List<Lead> list() { return leads.findAllByOrderByCreatedAtDesc(); }
    public long count() { return leads.count(); }
    public Lead create(LeadRequest form) {
        if ((form.phone() == null || form.phone().isBlank()) && (form.email() == null || form.email().isBlank()))
            throw new IllegalArgumentException("Укажите телефон или почту");
        Lead lead = new Lead();
        lead.setName(form.name().trim());
        lead.setPhone(form.phone() == null ? null : form.phone().trim());
        lead.setEmail(form.email() == null ? null : form.email().trim());
        lead.setMessage(form.message().trim());
        lead.setContactMethod(form.contactMethod());
        lead.setAmoCrmSyncStatus(amoCrm.isReady() ? "PENDING" : "WAITING_FOR_AUTH");
        lead = leads.save(lead);
        if ("PENDING".equals(lead.getAmoCrmSyncStatus())) amoCrm.sendLead(lead.getId());
        return lead;
    }
    public Lead find(Long id) { return leads.findById(id).orElseThrow(); }
    public void setProcessed(Long id, boolean processed) {
        Lead lead = leads.findById(id).orElseThrow();
        lead.setProcessed(processed);
        leads.save(lead);
    }
    public void delete(Long id) { leads.deleteById(id); }
}
