Inicialize o repositório git e configure o remote:

1. Verifique se já é um repo git (`git status`). Se não for, execute `git init`.
2. Verifique se o remote `origin` existe (`git remote -v`). Se não existir, pergunte a URL ao usuário e adicione com `git remote add origin <url>`.
3. Configure o credential store se o usuário fornecer um token.
4. Faça `git fetch origin` para sincronizar.
5. Configure o branch main para rastrear o remoto.
6. Mostre o status final.
