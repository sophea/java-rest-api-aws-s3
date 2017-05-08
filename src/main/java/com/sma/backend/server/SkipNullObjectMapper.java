package com.sma.backend.server;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SkipNullObjectMapper extends ObjectMapper {

	public void init() {
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
