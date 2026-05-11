package com.nieqi.nieqicodefather.ai;

import com.nieqi.nieqicodefather.model.enums.CodeGenTypeEnum;

public interface AiCodeGenTypeRoutingService {

    CodeGenTypeEnum routeCodeGenType(String userMessage);
}

