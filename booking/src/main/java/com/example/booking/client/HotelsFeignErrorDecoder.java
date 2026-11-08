package com.example.booking.client;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

public class HotelsFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 500 || status == 429) {
            return new RetryableException(
                    status,
                    "hotels-service temporary failure: HTTP " + status,
                    response.request().httpMethod(),
                    (Long) null,
                    response.request()
            );
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
