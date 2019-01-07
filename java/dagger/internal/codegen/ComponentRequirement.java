/*
 * Copyright (C) 2016 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static dagger.internal.codegen.DaggerElements.isAnyAnnotationPresent;
import static dagger.internal.codegen.SourceFiles.simpleVariableName;
import static dagger.internal.codegen.Util.componentCanMakeNewInstances;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import dagger.Binds;
import dagger.BindsOptionalOf;
import dagger.Provides;
import dagger.model.BindingKind;
import dagger.model.Key;
import dagger.multibindings.Multibinds;
import dagger.producers.Produces;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/** A type that a component needs an instance of. */
@AutoValue
abstract class ComponentRequirement {
  enum Kind {
    /** A type listed in the component's {@code dependencies} attribute. */
    DEPENDENCY,

    /** A type listed in the component or subcomponent's {@code modules} attribute. */
    MODULE,

    /**
     * An object that is passed to a builder's {@link dagger.BindsInstance @BindsInstance} method.
     */
    BOUND_INSTANCE,
    ;

    boolean isBoundInstance() {
      return equals(BOUND_INSTANCE);
    }

    boolean isModule() {
      return equals(MODULE);
    }
  }

  /** The kind of requirement. */
  abstract Kind kind();

  /** Returns true if this is a {@link Kind#BOUND_INSTANCE} requirement. */
  // TODO(ronshapiro): consider removing this and inlining the usages
  final boolean isBoundInstance() {
    return kind().isBoundInstance();
  }

  /**
   * The type of the instance the component must have, wrapped so that requirements can be used as
   * value types.
   */
  abstract Equivalence.Wrapper<TypeMirror> wrappedType();

  /** The type of the instance the component must have. */
  TypeMirror type() {
    return wrappedType().get();
  }

  /** The element associated with the type of this requirement. */
  TypeElement typeElement() {
    return MoreTypes.asTypeElement(type());
  }

  /** The action a component builder should take if it {@code null} is passed. */
  enum NullPolicy {
    /** Make a new instance. */
    NEW,
    /** Throw an exception. */
    THROW,
    /** Allow use of null values. */
    ALLOW,
  }

  /**
   * An override for the requirement's null policy. If set, this is used as the null policy instead
   * of the default behavior in {@link #nullPolicy}.
   *
   * <p>Some implementations' null policy can be determined upon construction (e.g., for binding
   * instances), but others' require Elements and Types, which must wait until {@link #nullPolicy}
   * is called.
   */
  abstract Optional<NullPolicy> overrideNullPolicy();

  /** The requirement's null policy. */
  NullPolicy nullPolicy(DaggerElements elements, DaggerTypes types) {
    if (overrideNullPolicy().isPresent()) {
      return overrideNullPolicy().get();
    }
    switch (kind()) {
      case MODULE:
        return componentCanMakeNewInstances(typeElement())
            ? NullPolicy.NEW
            : requiresAPassedInstance(elements, types) ? NullPolicy.THROW : NullPolicy.ALLOW;
      case DEPENDENCY:
      case BOUND_INSTANCE:
        return NullPolicy.THROW;
    }
    throw new AssertionError();
  }

  /**
   * Returns true if the passed {@link ComponentRequirement} requires a passed instance in order to
   * be used within a component.
   */
  boolean requiresAPassedInstance(DaggerElements elements, DaggerTypes types) {
    if (isBoundInstance()) {
      // A user has explicitly defined in their component builder they will provide an instance.
      return true;
    }

    ImmutableSet<ExecutableElement> methods =
        getLocalAndInheritedMethods(typeElement(), types, elements);
    boolean foundInstanceMethod = false;
    for (ExecutableElement method : methods) {
      if (method.getModifiers().contains(ABSTRACT)
          && !isAnyAnnotationPresent(
              method, Binds.class, Multibinds.class, BindsOptionalOf.class)) {
        // TODO(ronshapiro): it would be cool to have internal meta-annotations that could describe
        // these, like @AbstractBindingMethod
        /* We found an abstract method that isn't a binding method. That automatically means that
         * a user will have to provide an instance because we don't know which subclass to use. */
        return true;
      } else if (!method.getModifiers().contains(STATIC)
          && isAnyAnnotationPresent(method, Provides.class, Produces.class)) {
        foundInstanceMethod = true;
      }
    }

    if (foundInstanceMethod) {
      return !componentCanMakeNewInstances(typeElement());
    }

    return false;
  }

  /** The key for this requirement, if one is available. */
  abstract Optional<Key> key();

  /** Returns the name for this requirement that could be used as a variable. */
  abstract String variableName();

  static ComponentRequirement forDependency(TypeMirror type) {
    return new AutoValue_ComponentRequirement(
        Kind.DEPENDENCY,
        MoreTypes.equivalence().wrap(checkNotNull(type)),
        Optional.empty(),
        Optional.empty(),
        simpleVariableName(MoreTypes.asTypeElement(type)));
  }

  static ComponentRequirement forModule(TypeMirror type) {
    return new AutoValue_ComponentRequirement(
        Kind.MODULE,
        MoreTypes.equivalence().wrap(checkNotNull(type)),
        Optional.empty(),
        Optional.empty(),
        simpleVariableName(MoreTypes.asTypeElement(type)));
  }

  static ComponentRequirement forBoundInstance(Key key, boolean nullable, String variableName) {
    return new AutoValue_ComponentRequirement(
        Kind.BOUND_INSTANCE,
        MoreTypes.equivalence().wrap(key.type()),
        nullable ? Optional.of(NullPolicy.ALLOW) : Optional.empty(),
        Optional.of(key),
        variableName);
  }

  static ComponentRequirement forBoundInstance(ContributionBinding binding) {
    checkArgument(binding.kind().equals(BindingKind.BOUND_INSTANCE));
    return forBoundInstance(
        binding.key(),
        binding.nullableType().isPresent(),
        binding.bindingElement().get().getSimpleName().toString());
  }
}