package com.onlinemenu.systemmessage.dto;

/**
 * @param search          part of the code
 * @param prefix          start of the code
 * @param missingLanguage only messages with no text in this language
 */
public record SystemMessageFilter(String search, String prefix, String missingLanguage) {

}
