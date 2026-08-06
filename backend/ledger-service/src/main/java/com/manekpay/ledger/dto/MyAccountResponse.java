package com.manekpay.ledger.dto;

import java.util.List;
import java.util.UUID;

public record MyAccountResponse(UUID accountId, String accountNumber, List<WalletDto> wallets) {
}
