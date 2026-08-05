package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.ProxyType;
import com.manekpay.ledger.exception.DuplicateProxyException;
import com.manekpay.ledger.exception.ProxyNotFoundException;
import com.manekpay.ledger.repository.AccountProxyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProxyService {

    private final AccountProxyRepository proxyRepository;

    public ProxyService(AccountProxyRepository proxyRepository) {
        this.proxyRepository = proxyRepository;
    }

    @Transactional
    public AccountProxy linkProxy(UUID accountId, ProxyType type, String value) {
        if (proxyRepository.existsByTypeAndValue(type, value)) {
            throw new DuplicateProxyException();
        }
        // The existsByTypeAndValue check above doesn't prevent a genuine race between two
        // concurrent requests linking the same identifier - the unique(type, value) index
        // (V3 migration) is the real guard. saveAndFlush forces the insert (and any
        // constraint violation) to happen synchronously here, not at a later, uncatchable
        // flush point, so the race surfaces as the correct 409 instead of a raw 500.
        try {
            return proxyRepository.saveAndFlush(new AccountProxy(accountId, type, value));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateProxyException();
        }
    }

    public List<AccountProxy> listProxies(UUID accountId) {
        return proxyRepository.findByAccountId(accountId);
    }

    @Transactional
    public void deleteProxy(UUID accountId, UUID proxyId) {
        AccountProxy proxy = proxyRepository.findById(proxyId)
                .filter(p -> p.getAccountId().equals(accountId))
                .orElseThrow(ProxyNotFoundException::new);
        proxyRepository.delete(proxy);
    }
}
