package org.example.app.presentation;

import org.example.app.metier.IMetier;
import org.example.framework.core.ApplicationContext;
import org.example.framework.core.XmlApplicationContext;

/**
 * Demonstrates dependency injection configured via {@code beans.xml} (XML / JAXB).
 *
 * <p>Three beans are declared in the XML file to showcase all three injection modes:
 * <ul>
 *   <li>{@code metierSetter}  — setter injection</li>
 *   <li>{@code metierConstructor} — constructor injection</li>
 *   <li>{@code metierField} — field injection</li>
 * </ul>
 */
public class PresentationXml {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  XML ApplicationContext (JAXB/OXM)");
        System.out.println("========================================");

        ApplicationContext ctx = new XmlApplicationContext("beans.xml");

        System.out.println("\n--- Setter injection ---");
        IMetier metierSetter = (IMetier) ctx.getBean("metierSetter");
        System.out.println("Résultat : " + metierSetter.calcul());

        System.out.println("\n--- Constructor injection ---");
        IMetier metierCtor = (IMetier) ctx.getBean("metierConstructor");
        System.out.println("Résultat : " + metierCtor.calcul());

        System.out.println("\n--- Field injection ---");
        IMetier metierField = (IMetier) ctx.getBean("metierField");
        System.out.println("Résultat : " + metierField.calcul());
    }
}
