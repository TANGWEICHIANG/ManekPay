package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.ProxyType;
import com.manekpay.ledger.exception.DuplicateProxyException;
import com.manekpay.ledger.exception.ProxyNotFoundException;
import com.manekpay.ledger.repository.AccountProxyRepository;
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
        return proxyRepository.save(new AccountProxy(accountId, type, value));
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
