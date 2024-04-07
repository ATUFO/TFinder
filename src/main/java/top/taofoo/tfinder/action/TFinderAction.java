package top.taofoo.tfinder.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import top.taofoo.tfinder.action.dto.FieldUsageDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TFinderAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile data = e.getData(LangDataKeys.PSI_FILE);
        PsiClass[] classes = ((com.intellij.psi.PsiJavaFile) data).getClasses();
        Map<PsiMethod, List<FieldUsageDTO>> map = new HashMap<>();
        processClass(e, classes[0], map);
        System.out.println(map);
    }

    private static void processClass(@NotNull AnActionEvent e, PsiClass clazzElement, Map<PsiMethod, List<FieldUsageDTO>> map) {
        if(clazzElement.getExtendsList() == null){
            return;
        }
        PsiMethod[] methods = clazzElement.getMethods();
        for (PsiMethod method : methods) {
            if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                if (method.getReturnType() instanceof PsiPrimitiveType) {
                    GlobalSearchScope searchScope = GlobalSearchScopes.projectProductionScope(e.getProject());
                    Query<PsiReference> search = ReferencesSearch.search(method, searchScope);
                    String replace = method.getName().replace("get", "").replace("is", "");
                    List<FieldUsageDTO> collect = search.findAll().stream().map(element -> {
                        FieldUsageDTO fieldUsageDTO = new FieldUsageDTO();
                        fieldUsageDTO.setFileName(replace);
                        fieldUsageDTO.setFileName(method.getContainingFile().getName());
                        return fieldUsageDTO;
                    }).collect(Collectors.toList());
                    map.put(method, collect);
                } else if (method.getReturnType() instanceof PsiClassType) {
                    PsiClass psiClass = JavaPsiFacade.getInstance(e.getProject()).findClass(method.getReturnType().getCanonicalText(),GlobalSearchScope.allScope(e.getProject()));
                    if (!map.containsKey(method)) {
                        processClass(e, psiClass, map);
                    }
                } else if (method.getReturnType() instanceof PsiArrayType) {
                    PsiType componentType = ((PsiArrayType) method.getReturnType()).getComponentType();
                    System.out.println(componentType);
                }
            }
        }
    }
}
