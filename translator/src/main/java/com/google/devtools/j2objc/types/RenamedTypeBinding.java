/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.devtools.j2objc.types;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A dynamic proxy class that replaces name references to a type binding
 * with a different name, and optionally specify a different declaring
 * (outer) class.
 *
 * @author Tom Ball
 */
public class RenamedTypeBinding implements InvocationHandler {
  private final String newName;
  private final ITypeBinding declaringClass;
  private final ITypeBinding delegate;
  private final int modifiers;

  /**
   * Return a renamed type binding.
   *
   * @param newName the new name of the type
   * @param originalBinding the existing binding, normally generated by JDT
   */
  public static ITypeBinding rename(String newName, ITypeBinding originalBinding) {
    return rename(newName, originalBinding.getDeclaringClass(), originalBinding);
  }

  /**
   * Return a renamed type binding, with a different declaring (outer) class.
   *
   * @param newName the new name of the type
   * @param newDeclaringClass the outer class for this type, or null if there
   *            isn't one
   * @param originalBinding the existing binding, normally generated by JDT
   */
  public static ITypeBinding rename(String newName, ITypeBinding newDeclaringClass,
      ITypeBinding originalBinding) {
    return rename(newName, newDeclaringClass, originalBinding, originalBinding.getModifiers());
  }

  /**
   * Return a renamed type binding, with a different declaring (outer) class.
   *
   * @param newName the new name of the type
   * @param newDeclaringClass the outer class for this type, or null if there
   *            isn't one
   * @param originalBinding the existing binding, normally generated by JDT
   */
  public static ITypeBinding rename(String newName, ITypeBinding newDeclaringClass,
      ITypeBinding originalBinding, int modifiers) {
    Class<?> delegateClass = originalBinding.getClass();
    newDeclaringClass = Types.getRenamedBinding(newDeclaringClass); // may also be renamed
    return (ITypeBinding) Proxy.newProxyInstance(delegateClass.getClassLoader(),
        delegateClass.getInterfaces(),
        new RenamedTypeBinding(newName, newDeclaringClass, originalBinding, modifiers));
  }

  private RenamedTypeBinding(String newName, ITypeBinding declaringClass,
      ITypeBinding delegate, int modifiers) {
    this.newName = newName;
    this.declaringClass = declaringClass;
    this.delegate = delegate;
    this.modifiers = modifiers;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (methodName.equals("getName")) {
      return newName;
    }
    if (methodName.equals("getQualifiedName")) {
      String fullName = delegate.getQualifiedName();
      int lastPackageDelimiter = fullName.lastIndexOf('.');
      if (lastPackageDelimiter > 0) {
        return fullName.substring(0, lastPackageDelimiter + 1) + newName;
      } else {
        String pkgName = delegate.getPackage().getName();
        if (!pkgName.isEmpty()) {
          return pkgName + '.' + newName;
        }
      }
      return newName;
    }
    if (methodName.equals("getDeclaringClass")) {
      return declaringClass;
    }
    if (methodName.equals("getErasure")) {
      return proxy;
    }
    if (methodName.equals("getModifiers")) {
      return modifiers;
    }
    return method.invoke(delegate, args);
  }
}