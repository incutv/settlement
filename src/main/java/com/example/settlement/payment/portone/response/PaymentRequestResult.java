package com.example.settlement.payment.portone.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
public class PaymentRequestResult implements Serializable {

    private static final Integer SUCCESS_CODE = 0;
    private Integer code;
    private String message;
    private Object response;

    protected PaymentRequestResult(Integer code, String message, Object response) {
        this.code = code;
        this.message = message;
        this.response = response;
    }

    public Object getResponse() {
        if (isSuccess()) {
            return response;
        }
        throw new RuntimeException(message);
    }

    private boolean isSuccess() {
        return code.equals(SUCCESS_CODE);
    }

}
