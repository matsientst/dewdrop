package com.dewdrop.config.ascii;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Ascii {

    public static void writeAscii() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + " ______   _______  _     _  ______   ______    _______  _______ \n" + "|      | |       || | _ | ||      | |    _ |  |       ||       |\n" + "|  _    ||    ___|| || || ||  _    ||   | ||  |   _   ||    _  |\n"
                        + "| | |   ||   |___ |       || | |   ||   |_||_ |  | |  ||   |_| |\n" + "| |_|   ||    ___||       || |_|   ||    __  ||  |_|  ||    ___|\n" + "|       ||   |___ |   _   ||       ||   |  | ||       ||   |    \n"
                        + "|______| |_______||__| |__||______| |___|  |_||_______||___|    \n");
        log.info(sb.toString());
    }
}
