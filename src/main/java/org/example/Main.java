package org.example;

import org.example.app.presentation.PresentationAnnotations;
import org.example.app.presentation.PresentationXml;

/**
 * Entry point — runs both demonstration modes back to back.
 */
public class Main {
    public static void main(String[] args) {
        PresentationXml.main(args);
        System.out.println();
        PresentationAnnotations.main(args);
    }
}