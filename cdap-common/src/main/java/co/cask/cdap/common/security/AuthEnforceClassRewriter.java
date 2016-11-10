/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.common.security;

import co.cask.cdap.common.lang.ClassRewriter;
import co.cask.cdap.proto.id.EntityId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.proto.security.Action;
import co.cask.cdap.proto.security.Principal;
import co.cask.cdap.security.spi.authentication.AuthenticationContext;
import co.cask.cdap.security.spi.authorization.AuthorizationEnforcer;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ClassRewriter} for rewriting bytecode of classes which needs Authorization Enforcement.
 */
public class AuthEnforceClassRewriter implements ClassRewriter {

  private static final Type AUTHORIZATION_ENFORCER_TYPE = Type.getType(AuthorizationEnforcer.class);
  private static final Type AUTHENTICATION_CONTEXT_TYPE = Type.getType(AuthenticationContext.class);

  @Override
  public byte[] rewriteClass(String className, InputStream input) throws IOException {
    ClassReader cr = new ClassReader(input);
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    cr.accept(new AuthEnforceClassVisitor(className, cw), ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }

  private final class AuthEnforceClassVisitor extends ClassVisitor {

    private final String className;
    private final Type classType;

    AuthEnforceClassVisitor(String className, ClassWriter cw) {
      super(Opcodes.ASM5, cw);
      this.className = className;
      this.classType = Type.getObjectType(className.replace(".", "/"));
    }

    @Override
    public MethodVisitor visitMethod(final int access, String name, String desc, String signature,
                                     String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

      return new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {
        private boolean hasAuthEnforce;
        private List<String> entities = new ArrayList<>();
        private Class enforceOn;
        private List<String> actions = new ArrayList<>();


        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
          AnnotationVisitor av = super.visitAnnotation(desc, visible);
          if (visible) {
            String annotation = Type.getType(desc).getClassName();
            // if the annotation is present then visit the annotation
            if (AuthEnforce.class.getName().equals(annotation)) {
              hasAuthEnforce = true;
              av = new AnnotationVisitor(Opcodes.ASM5, av) {
                @Override
                public void visit(String name, Object value) {
                  try {
                    enforceOn = Class.forName(((Type) value).getClassName());
                  } catch (ClassNotFoundException e) {
                    Throwables.propagate(e);
                  }
                  super.visit(name, value);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                  AnnotationVisitor av = super.visitArray(name);
                  return new AnnotationVisitor(Opcodes.ASM5, av) {
                    @Override
                    public void visit(String name, Object value) {
                      entities.add((String) value);
                      super.visit(name, value);
                    }

                    @Override
                    public void visitEnum(String name, String desc, String value) {
                      actions.add(value);
                      super.visitEnum(name, desc, value);
                    }
                  };
                }
              };
            }
          }
          return av;
        }

        @Override
        protected void onMethodEnter() {
          if (hasAuthEnforce) {
            System.out.println("### AuthEnforce annotation found in class: " + className + " for \nEntities: " +
                                 entities + "enforceOn: " + enforceOn + " actions: " + actions);

//            // auth context print
//            getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
//            loadThis();
//            getField(classType, "authenticationContext", AUTHENTICATION_CONTEXT_TYPE);
//            invokeInterface(AUTHENTICATION_CONTEXT_TYPE,
//                            new Method("getPrincipal", Type.getMethodDescriptor(Type.getType(Principal.class))));
//            invokeVirtual(Type.getType(Principal.class),
//                            new Method("toString", Type.getMethodDescriptor(Type.getType(String.class))));
//            invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (String)"));
//
//
//            // entity print
//            getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
//            visitVarInsn(ALOAD, 1);
//            invokeVirtual(Type.getType(EntityId.class),
//                          new Method("toString", Type.getMethodDescriptor(Type.getType(String.class))));
//            invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (String)"));
//
//            // action print
//            getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
//            getStatic(Type.getType(Action.class), actions.get(0).toUpperCase(), Type.getType(Action.class));
//            invokeVirtual(Type.getType(Action.class),
//                          new Method("toString", Type.getMethodDescriptor(Type.getType(String.class))));
//            invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (String)"));

            // this.authorizationEnforcer
            loadThis();
            getField(classType, "authorizationEnforcer", AUTHORIZATION_ENFORCER_TYPE);
            // push the parameters to stack
            // entity id
            visitVarInsn(ALOAD, 1);
            // this.authenticationContext
            loadThis();
            getField(classType, "authenticationContext", AUTHENTICATION_CONTEXT_TYPE);
            // call getPrincipal on this.authenticationContext
            invokeInterface(AUTHENTICATION_CONTEXT_TYPE,
                            new Method("getPrincipal", Type.getMethodDescriptor(Type.getType(Principal.class))));
            // push action to stack
            getStatic(Type.getType(Action.class), actions.get(0).toUpperCase(), Type.getType(Action.class));
            // call enforce on this.authorizationEnforcer with above parameters
            invokeInterface(AUTHORIZATION_ENFORCER_TYPE,
                            new Method("enforce", Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                           Type.getType(EntityId.class),
                                                                           Type.getType(Principal.class),
                                                                           Type.getType(Action.class))));
          }
        }

        <T extends EntityId> EntityId getEntity(List<String> entityString, Class<T> idClass) {
          if (idClass.isAssignableFrom(NamespaceId.class)) {
            Preconditions.checkArgument(entityString.size() == 1);
            return new NamespaceId(entityString.get(0));
          } else {
            throw new RuntimeException("Not Supported type");
          }
        }
      };
    }
  }
}
