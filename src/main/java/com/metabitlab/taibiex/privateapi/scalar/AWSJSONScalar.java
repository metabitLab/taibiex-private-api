package com.metabitlab.taibiex.privateapi.scalar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netflix.graphql.dgs.DgsScalar;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

@DgsScalar(name = "AWSJSON")
public class AWSJSONScalar implements Coercing<JSONObject, String> {

    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        return dataFetcherResult.toString();
    }

    @Override
    public JSONObject parseValue(Object input) throws CoercingParseValueException {
        return JSON.parseObject(input.toString());
    }

    @Override
    public JSONObject parseLiteral(Object input) throws CoercingParseLiteralException {
        return JSON.parseObject(input.toString());
    }
}
