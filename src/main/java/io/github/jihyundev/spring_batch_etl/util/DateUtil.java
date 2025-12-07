package io.github.jihyundev.spring_batch_etl.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtil {

    //string -> 2011-12-03T10:15:30'
    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
