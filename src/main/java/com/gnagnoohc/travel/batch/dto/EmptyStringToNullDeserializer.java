package com.gnagnoohc.travel.batch.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/* EmptyStringToNullDeserializer.java
* 공공데이터 응답 특성상 결과 데이터가 없을 때 items 필드가 빈 문자열("")로 내려오는 경우가 존재,
* 방어로직이 필요한 경우에만 null로 안전하게 처리하고, 정상적인 객체(JSON)가 오면 원래대로 파싱을 이어가도록 하는 커스텀 디시리얼라이저

* ContextualDeserializer - 제네릭 필드(Items<T>)의 실제 타입을 Jackson이 알려주도록 하여
  정상 데이터가 왔을 때 타입을 몰라서 NPE 나는 문제 해결 (ctxt.getContextualType()이 null을 반환하던 문제) */
public class EmptyStringToNullDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

    // 이 필드가 실제로 채워야 하는 타입 (예: Items<TourLdongCodeDTO>) - createContextual에서 채워짐
    private final JavaType targetType;

    public EmptyStringToNullDeserializer() {
        super(Object.class);
        this.targetType = null;
    }

    public EmptyStringToNullDeserializer(JavaType targetType) {
        super(Object.class);
        this.targetType = targetType;
    }

    // Jackson이 실제 필드를 파싱하기 직전에 호출 - 그 필드의 진짜 제네릭 타입 정보를 여기서 넘겨받음
    @Override
    public com.fasterxml.jackson.databind.JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();
        return new EmptyStringToNullDeserializer(type);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 값이 빈 문자열("")인 경우에만 null 반환
        if (p.currentToken() != null && p.currentToken().isScalarValue()
                && p.getValueAsString() != null && p.getValueAsString().isEmpty()) {
            return null;
        }
        // 그 외(정상 객체)의 경우 createContextual에서 미리 받아둔 실제 타입(targetType)으로 위임 파싱
        return ctxt.readValue(p, targetType);
    }
}