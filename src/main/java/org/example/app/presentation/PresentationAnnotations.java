package org.example.app.presentation;

import org.example.app.metier.IMetier;
import org.example.framework.core.AnnotationApplicationContext;
import org.example.framework.core.ApplicationContext;

/**
 * Demonstrates dependency injection driven entirely by annotations
 * ({@code @Component}, {@code @Autowired}, {@code @Value}).
 *
 * <p>The container scans the {@code org.example.app} package and wires everything
 * automatically — no XML required.
 */
public class PresentationAnnotations {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Annotation ApplicationContext");
        System.out.println("========================================");

        ApplicationContext ctx = new AnnotationApplicationContext("org.example.app");

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
