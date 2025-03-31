package org.openrewrite.java.dropwizard.method;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.openrewrite.java.dropwizard.method.util.MethodStubCreator.buildMethodStub;
import static org.openrewrite.java.tree.Flag.Abstract;
import static org.openrewrite.java.tree.Flag.Default;
import static org.openrewrite.java.tree.TypeUtils.asFullyQualified;
import static org.openrewrite.java.tree.TypeUtils.isAssignableTo;

public class AddMissingAbstractMethods extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add missing abstract methods";
    }

    @Override
    public String getDescription() {
        return "Implements missing abstract methods from superclasses and interfaces.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddMissingMethodsVisitor();
    }

    public static class AddMissingMethodsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static List<JavaType.Method> getAbstractMethods(JavaType.FullyQualified implType) {
            if (implType.getMethods() == null) {
                return Collections.emptyList();
            }

            return implType.getMethods().stream()
                    .filter(m -> m.hasFlags(Abstract))
                    .filter(m -> !m.hasFlags(Default))
                    .collect(Collectors.toList());
        }

        private static Set<String> getObjectClassMethods() {
            Set<String> objectMethodNames = new HashSet<>();
            objectMethodNames.add("equals");
            objectMethodNames.add("hashCode");
            objectMethodNames.add("toString");
            objectMethodNames.add("clone");
            objectMethodNames.add("finalize");
            objectMethodNames.add("getClass");
            objectMethodNames.add("notify");
            objectMethodNames.add("notifyAll");
            objectMethodNames.add("wait");
            return objectMethodNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

            if (isNull(cd.getType())) {
                return cd;
            }

            List<JavaType.Method> abstractMethods = new ArrayList<>();

            JavaType.FullyQualified extendsType =
                    asFullyQualified(nonNull(cd.getExtends()) ? cd.getExtends().getType() : null);

            if (nonNull(extendsType)) {
                abstractMethods.addAll(getAbstractMethods(extendsType));
            }

            if (nonNull(cd.getImplements())) {
                for (TypeTree impl : cd.getImplements()) {
                    JavaType.FullyQualified implType = asFullyQualified(impl.getType());
                    if (nonNull(implType)) {
                        abstractMethods.addAll(getAbstractMethods(implType));
                    }
                }
            }

            List<JavaType.Method> missingMethods = findMissingAbstractMethods(cd, abstractMethods);

            if (missingMethods.isEmpty()) {
                return cd;
            }

            for (JavaType.Method missingMethod : missingMethods) {
                cd = createMethod(cd, missingMethod);
            }

            return maybeAutoFormat(classDecl, cd, ctx);
        }

        public J.ClassDeclaration createMethod(J.ClassDeclaration cd, JavaType.Method methodType) {
            if (methodType == null) {
                return cd;
            }

            JavaTemplate template =
                    JavaTemplate.builder(buildMethodStub(methodType)).contextSensitive().build();

            return template.apply(updateCursor(cd), cd.getBody().getCoordinates().lastStatement());
        }

        private List<JavaType.Method> findMissingAbstractMethods(
                J.ClassDeclaration classDecl, List<JavaType.Method> candidateAbstractMethods) {
            JavaType.FullyQualified classType = classDecl.getType();
            if (classType == null) {
                return Collections.emptyList();
            }

            // Get all method declarations from the class body
            List<J.MethodDeclaration> existingMethodDecls =
                    classDecl.getBody().getStatements().stream()
                            .filter(statement -> statement instanceof J.MethodDeclaration)
                            .map(statement -> (J.MethodDeclaration) statement)
                            .collect(Collectors.toList());

            List<JavaType.Method> missing = new ArrayList<>();
            Set<String> objectMethodNames = getObjectClassMethods();

            for (JavaType.Method abstractMethod : candidateAbstractMethods) {
                if (objectMethodNames.contains(abstractMethod.getName())) {
                    continue;
                }

                boolean alreadyImplemented =
                        existingMethodDecls.stream()
                                .anyMatch(
                                        methodDecl -> {
                                            JavaType.Method methodType = methodDecl.getMethodType();
                                            return methodType != null && signaturesMatch(methodType, abstractMethod);
                                        });

                if (!alreadyImplemented) {
                    missing.add(abstractMethod);
                }
            }

            return missing;
        }

        private boolean signaturesMatch(JavaType.Method existing, JavaType.Method candidate) {
            if (!existing.getName().equals(candidate.getName())) {
                return false;
            }

            if (existing.getParameterTypes().size() != candidate.getParameterTypes().size()) {
                return false;
            }

            // Then do a robust generic check
            // We need the parent's JavaType.Parameterized if it's generic, e.g. the parent's
            // "ComplexParent"
            JavaType parentType = existing.getDeclaringType();
            if (parentType instanceof JavaType.Parameterized) {
                return methodMatchesGenericDefinition(
                        existing, candidate, (JavaType.Parameterized) parentType);
            } else {
                // fallback: compare them in the simpler, old-fashioned way
                return isAssignableTo(existing.getReturnType(), candidate.getReturnType());
            }
        }

        private boolean areTypesCompatible(JavaType type1, JavaType type2) {
            if (type1 == null || type2 == null) {
                return type1 == type2;
            }

            // Get fully qualified names if possible
            String name1 = getFullyQualifiedName(type1);
            String name2 = getFullyQualifiedName(type2);

            if (name1 != null && name2 != null) {
                // Handle primitive type variations (e.g., int vs java.lang.Integer)
                name1 = normalizePrimitiveType(name1);
                name2 = normalizePrimitiveType(name2);
                return name1.equals(name2);
            }

            // Handle parameterized types
            if (type1 instanceof JavaType.Parameterized && type2 instanceof JavaType.Parameterized) {
                return areParameterizedTypesCompatible(
                        (JavaType.Parameterized) type1, (JavaType.Parameterized) type2);
            }

            // If we can't determine compatibility, err on the side of caution
            return false;
        }

        private String getFullyQualifiedName(JavaType type) {
            if (type instanceof JavaType.Primitive) {
                return ((JavaType.Primitive) type).getKeyword();
            }

            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            return fq != null ? fq.getFullyQualifiedName() : null;
        }

        private String normalizePrimitiveType(String typeName) {
            Map<String, String> primitiveToWrapper = new HashMap<>();
            primitiveToWrapper.put("int", "java.lang.Integer");
            primitiveToWrapper.put("long", "java.lang.Long");
            primitiveToWrapper.put("boolean", "java.lang.Boolean");
            primitiveToWrapper.put("byte", "java.lang.Byte");
            primitiveToWrapper.put("short", "java.lang.Short");
            primitiveToWrapper.put("char", "java.lang.Character");
            primitiveToWrapper.put("float", "java.lang.Float");
            primitiveToWrapper.put("double", "java.lang.Double");

            return primitiveToWrapper.getOrDefault(typeName, typeName);
        }

        private boolean areParameterizedTypesCompatible(
                JavaType.Parameterized type1, JavaType.Parameterized type2) {
            // Check base type compatibility
            if (!areTypesCompatible(type1.getType(), type2.getType())) {
                return false;
            }

            // Check type parameters
            List<JavaType> params1 = type1.getTypeParameters();
            List<JavaType> params2 = type2.getTypeParameters();

            if (params1.size() != params2.size()) {
                return false;
            }

            for (int i = 0; i < params1.size(); i++) {
                if (!areTypesCompatible(params1.get(i), params2.get(i))) {
                    return false;
                }
            }

            return true;
        }

        private boolean methodMatchesGenericDefinition(
                JavaType.Method existing, JavaType.Method candidate, JavaType.Parameterized declaringType) {

            // Compare parameter counts
            if (existing.getParameterTypes().size() != candidate.getParameterTypes().size()) {
                return false;
            }

            // Get type mappings from the parameterized type
            Map<JavaType, JavaType> typeMapping = new HashMap<>();
            JavaType.FullyQualified rawType = declaringType.getType();
            if (rawType instanceof JavaType.Class) {
                JavaType.Class classType = (JavaType.Class) rawType;
                List<JavaType> typeVarNames = classType.getTypeParameters();
                List<JavaType> typeArgs = declaringType.getTypeParameters();

                for (int i = 0; i < typeVarNames.size() && i < typeArgs.size(); i++) {
                    typeMapping.put(typeVarNames.get(i), typeArgs.get(i));
                }
            }

            // Check parameters
            for (int i = 0; i < existing.getParameterTypes().size(); i++) {
                JavaType existingParam = existing.getParameterTypes().get(i);
                JavaType candidateParam = candidate.getParameterTypes().get(i);

                if (candidateParam instanceof JavaType.GenericTypeVariable) {
                    JavaType.GenericTypeVariable genericParam = (JavaType.GenericTypeVariable) candidateParam;
                    String genericName = genericParam.getName();

                    // Check if this generic parameter has a mapping
                    if (typeMapping.containsKey(genericName)) {
                        JavaType mappedType = typeMapping.get(genericName);
                        if (!isAssignableTo(existingParam, mappedType)) {
                            return false;
                        }
                    } else {
                        // If no mapping exists, check bounds
                        List<JavaType> bounds = genericParam.getBounds();
                        if (!bounds.isEmpty()
                                && !bounds.stream().allMatch(bound -> isAssignableTo(existingParam, bound))) {
                            return false;
                        }
                    }
                } else if (!isAssignableTo(existingParam, candidateParam)) {
                    return false;
                }
            }

            // Check return type
            JavaType existingReturn = existing.getReturnType();
            JavaType candidateReturn = candidate.getReturnType();

            if (existingReturn instanceof JavaType.Parameterized
                    && candidateReturn instanceof JavaType.Parameterized) {
                JavaType.Parameterized existingParamReturn = (JavaType.Parameterized) existingReturn;
                JavaType.Parameterized candidateParamReturn = (JavaType.Parameterized) candidateReturn;

                // Compare the raw types (e.g., Optional)
                if (!isAssignableTo(existingParamReturn.getType(), candidateParamReturn.getType())) {
                    return false;
                }

                // Compare type arguments
                List<JavaType> existingTypeArgs = existingParamReturn.getTypeParameters();
                List<JavaType> candidateTypeArgs = candidateParamReturn.getTypeParameters();

                if (existingTypeArgs.size() != candidateTypeArgs.size()) {
                    return false;
                }

                for (int i = 0; i < existingTypeArgs.size(); i++) {
                    JavaType existingArg = existingTypeArgs.get(i);
                    JavaType candidateArg = candidateTypeArgs.get(i);

                    if (candidateArg instanceof JavaType.GenericTypeVariable) {
                        String genericName = ((JavaType.GenericTypeVariable) candidateArg).getName();
                        if (typeMapping.containsKey(genericName)) {
                            JavaType mappedType = typeMapping.get(genericName);
                            if (!isAssignableTo(existingArg, mappedType)) {
                                return false;
                            }
                        }
                    } else if (!isAssignableTo(existingArg, candidateArg)) {
                        return false;
                    }
                }
            } else return isAssignableTo(existingReturn, candidateReturn);

            return true;
        }
    }
}
