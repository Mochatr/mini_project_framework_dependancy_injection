# Mini Framework d'Injection de Dépendances — Compte Rendu

> **Module :** Architecture JEE et Systèmes Distribués  
> **Auteur :** Mochatr  
> **Année :** 2025 / 2026

---

## Table des matières

1. [Introduction et Objectifs](#1-introduction-et-objectifs)
2. [Rappel — Projet BDCC-IOC](#2-rappel--projet-bdcc-ioc)
3. [Architecture du projet](#3-architecture-du-projet)
4. [Le Framework — Conception](#4-le-framework--conception)
   - 4.1 [Interface `ApplicationContext`](#41-interface-applicationcontext)
   - 4.2 [Les Annotations](#42-les-annotations)
   - 4.3 [Version XML — `XmlApplicationContext`](#43-version-xml--xmlapplicationcontext)
   - 4.4 [Version Annotations — `AnnotationApplicationContext`](#44-version-annotations--annotationapplicationcontext)
5. [Modes d'injection supportés](#5-modes-dinjection-supportés)
   - 5.1 [Injection par Constructeur](#51-injection-par-constructeur)
   - 5.2 [Injection par Setter](#52-injection-par-setter)
   - 5.3 [Injection par Attribut (Field)](#53-injection-par-attribut-field)
6. [Application de démonstration](#6-application-de-démonstration)
7. [Configuration XML (`beans.xml`)](#7-configuration-xml-beansxml)
8. [Dépendances Maven](#8-dépendances-maven)
9. [Exécution et résultats](#9-exécution-et-résultats)
10. [Comparaison avec Spring IoC](#10-comparaison-avec-spring-ioc)
11. [Conclusion](#11-conclusion)

---

## 1. Introduction et Objectifs

Ce mini-projet prolonge le TP **BDCC-IOC** (Inversion de Contrôle) en implémentant, **from scratch**, un framework d'injection de dépendances similaire au conteneur IoC de Spring.

### Objectifs pédagogiques

| Objectif | Couvert |
|----------|---------|
| Comprendre le principe IoC / DI | ✅ |
| Implémenter un contexte XML (JAXB/OXM) | ✅ |
| Implémenter un contexte par Annotations | ✅ |
| Injection via Constructeur | ✅ |
| Injection via Setter | ✅ |
| Injection via Attribut (Field) | ✅ |

Le framework produit est **autonome** : il ne dépend pas de Spring, CDI ou Guice. Il illustre exactement ce que ces frameworks font sous le capot.

---

## 2. Rappel — Projet BDCC-IOC

Le projet BDCC-IOC montrait comment passer d'une instanciation **manuelle** des dépendances à une instanciation **externe** (via fichier de config ou annotations). Les étapes explorées étaient :

1. Instanciation directe (`new DaoImpl()`) — couplage fort
2. Instanciation dynamique via `Class.forName()` et un fichier `.properties`
3. Utilisation du conteneur Spring XML
4. Utilisation du conteneur Spring Annotations

Ce mini-projet **reproduit les étapes 3 et 4** avec notre propre implémentation.

---

## 3. Architecture du projet

```
src/main/
├── java/org/example/
│   ├── Main.java                              ← Point d'entrée (lance les deux démos)
│   │
│   ├── framework/                             ← LE FRAMEWORK (réutilisable)
│   │   ├── annotations/
│   │   │   ├── Component.java                 @Component("beanName")
│   │   │   ├── Autowired.java                 @Autowired
│   │   │   ├── Qualifier.java                 @Qualifier("id")
│   │   │   └── Value.java                     @Value("valeur littérale")
│   │   ├── core/
│   │   │   ├── ApplicationContext.java        Interface du conteneur IoC
│   │   │   ├── XmlApplicationContext.java     Implémentation XML (JAXB)
│   │   │   └── AnnotationApplicationContext.java  Implémentation Annotations
│   │   ├── xml/                               Modèle JAXB (OXM)
│   │   │   ├── BeansConfig.java               <beans> racine XML
│   │   │   ├── BeanDefinition.java            <bean id="..." class="...">
│   │   │   ├── PropertyDefinition.java        <property name="..." ref/value="..."/>
│   │   │   └── ConstructorArgDefinition.java  <constructor-arg ref/value="..."/>
│   │   └── exception/
│   │       └── BeanException.java             RuntimeException du conteneur
│   │
│   └── app/                                   APPLICATION DE DÉMONSTRATION
│       ├── dao/
│       │   ├── IDao.java                      Interface couche données
│       │   └── DaoImpl.java                   Implémentation (@Component)
│       ├── metier/
│       │   ├── IMetier.java                   Interface couche métier
│       │   ├── MetierImplSetter.java           Injection par setter (@Autowired sur setter)
│       │   ├── MetierImplConstructor.java      Injection par constructeur (@Autowired sur ctor)
│       │   └── MetierImplField.java            Injection par champ (@Autowired + @Value)
│       └── presentation/
│           ├── PresentationXml.java            Démo via XmlApplicationContext
│           └── PresentationAnnotations.java    Démo via AnnotationApplicationContext
│
└── resources/
    └── beans.xml                              Fichier de configuration XML
```

---

## 4. Le Framework — Conception

### 4.1 Interface `ApplicationContext`

```java
public interface ApplicationContext {
    Object getBean(String name);
    <T> T getBean(Class<T> type);
}
```

Inspirée de `org.springframework.context.ApplicationContext`, cette interface expose deux méthodes de lookup : par nom de bean ou par type.

---

### 4.2 Les Annotations

| Annotation | Cible | Rôle |
|------------|-------|------|
| `@Component("id")` | Classe | Déclare la classe comme bean géré. L'`id` est optionnel (défaut : nom de classe en camelCase). |
| `@Autowired` | Constructeur, Méthode, Champ | Point d'injection. Le conteneur résout et injecte le bean correspondant. |
| `@Qualifier("id")` | Champ, Paramètre | Désambiguïse quand plusieurs beans du même type existent. |
| `@Value("val")` | Champ, Paramètre | Injecte une valeur littérale (String, int, double, boolean…). |

---

### 4.3 Version XML — `XmlApplicationContext`

**Fonctionnement :**

1. Le fichier XML est **désérialisé** avec JAXB (`JAXBContext`, `Unmarshaller`) vers un objet `BeansConfig`.
2. Pour chaque `<bean>`, la classe est chargée via `Class.forName(className)`.
3. Si des `<constructor-arg>` sont présents → **injection par constructeur**.
4. Sinon → appel du constructeur no-arg.
5. Pour chaque `<property>` :
   - Si un setter `setXxx()` existe → **injection par setter**.
   - Sinon → accès direct au champ via `Field.setAccessible(true)` → **injection par champ**.
6. Les valeurs `ref="autreBean"` sont résolues récursivement (ordre de création géré).
7. Les valeurs `value="..."` sont **coercées** vers le type cible (int, long, double, boolean).

**Algorithme de résolution :**

```
pour chaque bean dans definitions :
    si bean pas encore créé :
        résoudre ses dépendances (récursif)
        instancier
        injecter les propriétés
        stocker dans la map singletons
```

---

### 4.4 Version Annotations — `AnnotationApplicationContext`

**Fonctionnement :**

1. La bibliothèque **Reflections** scanne le package de base pour trouver toutes les classes annotées `@Component`.
2. Pour chaque classe :
   - Si un constructeur est annoté `@Autowired` → **injection constructeur**.
   - Sinon → constructeur no-arg.
3. Après que toutes les instances sont créées :
   - Chaque méthode annotée `@Autowired` est appelée → **injection setter**.
   - Chaque champ annoté `@Autowired` ou `@Value` est injecté directement → **injection champ**.
4. La résolution par type utilise `Class.isAssignableFrom()` pour supporter les interfaces.
5. `@Qualifier` est consulté en priorité pour désambiguïser.

---

## 5. Modes d'injection supportés

### 5.1 Injection par Constructeur

**Via XML :**
```xml
<bean id="metierConstructor" class="org.example.app.metier.MetierImplConstructor">
    <constructor-arg ref="dao"/>
</bean>
```

**Via Annotations :**
```java
@Component("metierConstructor")
public class MetierImplConstructor implements IMetier {

    private final IDao dao;

    @Autowired
    public MetierImplConstructor(IDao dao) {
        this.dao = dao;
    }
}
```

> **Avantage :** Les dépendances sont **immutables** (`final`), ce qui favorise un design plus solide et facilite les tests unitaires.

---

### 5.2 Injection par Setter

**Via XML :**
```xml
<bean id="metierSetter" class="org.example.app.metier.MetierImplSetter">
    <property name="dao" ref="dao"/>
</bean>
```

**Via Annotations :**
```java
@Component("metierSetter")
public class MetierImplSetter implements IMetier {

    private IDao dao;

    @Autowired
    public void setDao(IDao dao) {
        this.dao = dao;
    }
}
```

> **Avantage :** Dépendances **optionnelles** ou modifiables après construction. Compatible avec les frameworks qui exigent un constructeur no-arg (ex : JPA).

---

### 5.3 Injection par Attribut (Field)

**Via XML** (fallback : pas de setter correspondant) :
```xml
<bean id="metierField" class="org.example.app.metier.MetierImplField">
    <property name="dao"        ref="dao"/>
    <property name="multiplier" value="10"/>
</bean>
```

**Via Annotations :**
```java
@Component("metierField")
public class MetierImplField implements IMetier {

    @Autowired
    private IDao dao;         // injection d'un bean

    @Value("10")
    private int multiplier;   // injection d'une valeur littérale
}
```

> **Note :** Ce mode utilise `Field.setAccessible(true)` pour contourner l'encapsulation. Il est le plus concis mais **déconseillé en production** car il cache les dépendances et complique les tests unitaires (pas de constructeur ou setter public pour mocker).

---

## 6. Application de démonstration

L'application suit la même architecture en couches que le projet BDCC-IOC :

```
Présentation → Métier → DAO
```

| Classe | Rôle |
|--------|------|
| `IDao` | Contrat de la couche d'accès aux données |
| `DaoImpl` | Simule un accès base de données (retourne `42.0`) |
| `IMetier` | Contrat de la couche métier |
| `MetierImplSetter` | Logique métier, dépendance injectée par setter |
| `MetierImplConstructor` | Logique métier, dépendance injectée par constructeur |
| `MetierImplField` | Logique métier, dépendances injectées par champs |
| `PresentationXml` | Lance le contexte XML et affiche les résultats |
| `PresentationAnnotations` | Lance le contexte Annotations et affiche les résultats |

---

## 7. Configuration XML (`beans.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <!-- Shared DAO bean (pas de dépendances) -->
    <bean id="dao" class="org.example.app.dao.DaoImpl"/>

    <!-- Setter injection -->
    <bean id="metierSetter" class="org.example.app.metier.MetierImplSetter">
        <property name="dao" ref="dao"/>
    </bean>

    <!-- Constructor injection -->
    <bean id="metierConstructor" class="org.example.app.metier.MetierImplConstructor">
        <constructor-arg ref="dao"/>
    </bean>

    <!-- Field injection (ref + valeur littérale) -->
    <bean id="metierField" class="org.example.app.metier.MetierImplField">
        <property name="dao"        ref="dao"/>
        <property name="multiplier" value="10"/>
    </bean>

</beans>
```

Le mapping JAXB est réalisé via les annotations `@XmlRootElement`, `@XmlElement`, `@XmlAttribute` sur les classes du package `framework.xml`.

---

## 8. Dépendances Maven

```xml
<!-- JAXB API + Runtime (retiré du JDK depuis Java 9) -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.2</version>
</dependency>
<dependency>
    <groupId>com.sun.xml.bind</groupId>
    <artifactId>jaxb-impl</artifactId>
    <version>4.0.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Scan de classpath pour les @Component -->
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
</dependency>
```

| Dépendance | Rôle |
|------------|------|
| `jakarta.xml.bind-api` | API JAXB (OXM) pour le mapping XML ↔ Objet |
| `jaxb-impl` | Implémentation JAXB (runtime) |
| `reflections` | Scan de packages pour détecter les classes `@Component` |

---

## 9. Exécution et résultats

### Compilation et exécution

```bash
mvn package
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
java -cp "target/Framework_injection_depondance-1.0-SNAPSHOT.jar;target/dependency/*" org.example.Main
```

### Sortie console

```
========================================
  XML ApplicationContext (JAXB/OXM)
========================================

--- Setter injection ---
[MetierImplSetter] Performing business calculation (setter injection)...
[DaoImpl] Fetching data from database...
Résultat : 84.0

--- Constructor injection ---
[MetierImplConstructor] Performing business calculation (constructor injection)...
[DaoImpl] Fetching data from database...
Résultat : 126.0

--- Field injection ---
[MetierImplField] Performing business calculation (field injection, multiplier=10)...
[DaoImpl] Fetching data from database...
Résultat : 420.0

========================================
  Annotation ApplicationContext
========================================

--- Setter injection ---
[MetierImplSetter] Performing business calculation (setter injection)...
[DaoImpl] Fetching data from database...
Résultat : 84.0

--- Constructor injection ---
[MetierImplConstructor] Performing business calculation (constructor injection)...
[DaoImpl] Fetching data from database...
Résultat : 126.0

--- Field injection ---
[MetierImplField] Performing business calculation (field injection, multiplier=10)...
[DaoImpl] Fetching data from database...
Résultat : 420.0
```

### Explication des calculs

| Bean | Formule | Résultat |
|------|---------|---------|
| `metierSetter` | `dao.getData() × 2 = 42.0 × 2` | **84.0** |
| `metierConstructor` | `dao.getData() × 3 = 42.0 × 3` | **126.0** |
| `metierField` | `dao.getData() × multiplier = 42.0 × 10` | **420.0** |

Les deux contextes produisent **exactement les mêmes résultats**, démontrant que XML et Annotations sont deux façons équivalentes de configurer le même conteneur IoC.

---

## 10. Comparaison avec Spring IoC

| Fonctionnalité | Notre Framework | Spring IoC |
|----------------|-----------------|------------|
| Conteneur XML | ✅ `XmlApplicationContext` | ✅ `ClassPathXmlApplicationContext` |
| Conteneur Annotations | ✅ `AnnotationApplicationContext` | ✅ `AnnotationConfigApplicationContext` |
| `@Component` | ✅ | ✅ |
| `@Autowired` | ✅ | ✅ |
| `@Qualifier` | ✅ | ✅ |
| `@Value` | ✅ (primitives/String) | ✅ (+ SpEL, properties files) |
| Injection constructeur | ✅ | ✅ |
| Injection setter | ✅ | ✅ |
| Injection champ | ✅ | ✅ |
| Scope singleton | ✅ | ✅ |
| Scope prototype | ❌ | ✅ |
| Cycle de vie (`@PostConstruct`) | ❌ | ✅ |
| AOP | ❌ | ✅ |
| `@Conditional` | ❌ | ✅ |
| Injection de collections | ❌ | ✅ |

Notre framework couvre le **cœur fonctionnel** du conteneur IoC de Spring, en omettant les fonctionnalités avancées (AOP, scopes multiples, SpEL) qui dépassent l'objectif pédagogique.

---

## 11. Conclusion

Ce mini-projet a permis de :

1. **Comprendre en profondeur** le mécanisme d'Inversion de Contrôle en l'implémentant manuellement.
2. **Maîtriser l'API Reflection** (`Class.forName`, `Constructor`, `Method`, `Field`, `setAccessible`) qui est le moteur de tout framework DI.
3. **Utiliser JAXB/OXM** pour le mapping XML ↔ Objet, technologie au cœur de la configuration XML de Spring.
4. **Concevoir une API propre** (`ApplicationContext`) qui découple la définition des beans de leur utilisation.
5. **Reproduire le comportement de Spring** de manière simplifiée, ce qui aide à démystifier ce framework largement utilisé en entreprise.

> *"Pour comprendre un framework, le meilleur exercice est de le réimplémenter."*
