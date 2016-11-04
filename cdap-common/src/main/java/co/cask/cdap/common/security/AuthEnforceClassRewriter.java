package co.cask.cdap.common.security;

import co.cask.cdap.common.lang.ClassRewriter;
import co.cask.cdap.proto.security.Action;
import co.cask.cdap.security.spi.authorization.AuthorizationEnforcer;
import com.google.common.base.Throwables;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ClassRewriter} for rewriting bytecode of classes which needs Authorization Enforcement.
 */
public class AuthEnforceClassRewriter implements ClassRewriter {

  private static final Type AUTHORIZATION_ENFORCER_TYPE = Type.getType(AuthorizationEnforcer.class);

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
    public MethodVisitor visitMethod(final int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

      return new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {
        private boolean hasAuthEnforce;
        private List<String> entities = new ArrayList<>();
        private Class enforceOn;
        private List<Action> actions = new ArrayList<>();


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
                      actions.add(Action.valueOf(value));
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
        public void visitCode() {
          if (hasAuthEnforce) {
            System.out.println("AuthEnforce annotation found in class: " + className + " for \nEntities: " + entities +
                                 "enforceOn: " + enforceOn + " actions: " + actions);
          }
          super.visitCode();
        }
      };
    }
  }
}
