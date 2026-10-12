package com.onlinemenu.billing.dto;

import java.util.List;

/**
 * @param callbackUrl  the address to register in the provider's merchant cabinet
 * @param envVariables server environment variables the provider reads its keys from
 */
public record ProviderSettingsDto(
    String code,
    String displayName,
    boolean configured,
    boolean enabled,
    String callbackUrl,
    List<String> envVariables
) {

}
