package com.loopers.application.order;

import com.loopers.domain.payment.model.CardType;

public record PaymentMethod(CardType cardType, String cardNo) {
}
