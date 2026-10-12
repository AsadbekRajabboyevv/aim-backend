package com.onlinemenu.ai.chat.dto;

import com.onlinemenu.billing.dto.UsageItemDto;

/**
 * @param messages this month's AI message allowance of the plan, null when the user is not limited
 */
public record AiStatusDto(
    boolean enabled,
    boolean configured,
    UsageItemDto messages
) {

}