Faça push das alterações para o repositório remoto:

1. Execute `git status` para verificar se há commits para enviar.
2. Execute `git log --oneline origin/main..HEAD` para ver os commits que serão enviados.
3. Se o branch local estiver atrás do remoto, faça `git pull --rebase origin main` primeiro para sincronizar.
4. Se houver conflitos no rebase, resolva aceitando a versão mais recente e continue.
5. Execute `git push origin main`.
6. Confirme o sucesso mostrando `git log --oneline -3`.
