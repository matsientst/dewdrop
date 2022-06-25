package com.dewdrop.config.ascii;

import java.time.YearMonth;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Ascii {

    public static void writeAscii() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(" ______   _______  _     _  ______   ______    _______  _______ \n");
        sb.append("|      | |       || | _ | ||      | |    _ |  |       ||       |\n");
        sb.append("|  _    ||    ___|| || || ||  _    ||   | ||  |   _   ||    _  |\n");
        sb.append("| | |   ||   |___ |       || | |   ||   |_||_ |  | |  ||   |_| |\n");
        sb.append("| |_|   ||    ___||       || |_|   ||    __  ||  |_|  ||    ___|\n");
        sb.append("|       ||   |___ |   _   ||       ||   |  | ||       ||   |    \n");
        sb.append("|______| |_______||__| |__||______| |___|  |_||_______||___|    \n");
        sb.append("\n");
        sb.append("A framework for event sourcing.");
        sb.append("\n");
        sb.append("Author: Matt Macchia");
        sb.append("\n");
        sb.append("Copyright: ").append(YearMonth.now().getYear());
        sb.append("\n");
        log.info(sb.toString());
    }
}
