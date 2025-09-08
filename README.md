# Staff Manager API

## Sobre

A Staff Manager API foi desenvolvida para ajudar o setor de Recursos Humanos (RH) de uma empresa a gerenciar times, funcionários e departamentos de forma simples e eficiente. Com ela, o RH pode centralizar as informações dos colaboradores, controlar a estrutura organizacional e facilitar a gestão operacional do quadro de funcionários, contribuindo para processos mais ágeis e organizados.

## Tabela de Conteúdos

- [Funcionalidades](#funcionalidades)  
- [Tecnologias](#tecnologias)  
- [Como Rodar](#como-rodar)  
- [Usuário Administrador Padrão](#usuário-administrador-padrão)  
- [Endpoints da API](#endpoints-da-api)  
  - [Como testar e visualizar os endpoints via Swagger](#como-testar-e-visualizar-os-endpoints-via-swagger)
- [Testes Unitários](#testes-unitários)   
- [Estado Atual](#estado-atual)  
- [Contribuição](#contribuição)  
- [Observações](#observações) 

## Funcionalidades

- Cadastro, edição, consulta e exclusão de departamentos
- Gestão de funcionários, envolvendo cadastro, edição, consulta, exclusão e upload/remoção de fotos
- Cadastro, consulta, edição e exclusão de usuários
- Configuração de segurança com Spring Security e filtro JWT customizado
- Documentação via Swagger
- Testes Unitários das classes DepartmentService, EmployeeService e UserService
- [Ainda não implementados] Testes de integração

## Tecnologias

- Java 21  
- Spring Boot 3.5.3 (Web, Data JPA, Security, Validation)  
- MySQL  
- Lombok  
- MapStruct  
- Auth0 Java JWT  
- Maven  
- SpringDoc OpenAPI/Swagger
- JUnit 5
- Mockito  

## Como Rodar

1. Clone o repositório:  
   ```bash
   git clone https://github.com/MarceloB-Junior/staff_manager_api.git
   ```
2. Certifique-se de que o Java 21 e o Maven estão instalados e configurados na sua máquina.  
3. Configure o banco de dados MySQL local com a database `staff_manager_db` e ajuste as credenciais no arquivo `application.properties` caso necessário.  
4. No terminal, navegue até o diretório do projeto e execute:  
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
5. A aplicação estará disponível em `http://localhost:8080`.

## Usuário Administrador Padrão

Ao iniciar a aplicação, um usuário administrador padrão é criado automaticamente para facilitar o acesso inicial e a gestão do sistema.

- Email: `john.doe@example.com`  
- Senha: `pwd123`  

> **Importante:** Por questões de segurança, recomenda-se alterar a senha padrão após o primeiro acesso ou configurar credenciais seguras para ambientes de produção.

## Endpoints da API

### Como testar e visualizar os endpoints via Swagger

A API possui documentação interativa gerada automaticamente via Swagger. Você pode acessar e testar todos os endpoints diretamente pelo navegador:

- Abra o navegador e vá para:   
  `http://localhost:8080/swagger-ui/index.html`

- Na interface do Swagger, todos os endpoints estão organizados por grupos, com detalhamento de parâmetros, exemplos de requisição e resposta.  
- Para testar um endpoint:  
 1. Clique no endpoint desejado para expandir.

    ![1](https://live.staticflickr.com/65535/54704318543_4fe3ff800c_h.jpg)

 2. Clique em **Try it out** para personalizar os dados da requisição.

    ![2](https://live.staticflickr.com/65535/54703289547_035e25c4f6_h.jpg)

 3. Preencha os parâmetros necessários (id, request body etc).

    ![3](https://live.staticflickr.com/65535/54703289517_8c77046a74_h.jpg)

 4. Clique em **Execute** para executar a requisição.

    ![4](https://live.staticflickr.com/65535/54703289382_c10a0749e3_h.jpg)

 5. Veja a resposta diretamente na interface.

    ![5](https://live.staticflickr.com/65535/54704449655_c7b4f0230e_h.jpg)

> **Dicas para o uso de autenticação via Swagger:**

> - Para endpoints protegidos, é possível informar o token JWT no campo "Authorize", localizado no canto superior direito da interface Swagger.
>
>  ![6](https://live.staticflickr.com/65535/54704351344_d3ef872294_h.jpg)
>
> - Clique em **Authorize**, cole o token e confirme, assim o Swagger incluirá o token nas requisições subsequentes.
>
>  ![7](https://live.staticflickr.com/65535/54704449620_4d95dbf69f_h.jpg)
  

## Testes Unitários
O projeto contém testes unitários para as principais classes de serviço, que ajudam a garantir o funcionamento correto das operações de criação, leitura, atualização e exclusão, além de validar os tratamentos de exceções.
- **DepartmentServiceTest**: Métodos testados incluem `save`, `findAll`, `findById`, `update` e `delete`. Os testes verificam cenários de sucesso, validação de nome duplicado, tratamento de departamentos não encontrados e remoção adequada da entidade e fotos associadas. Localização: `src/test/java/com/api/staff_manager/services/DepartmentServiceTest.java`
- **EmployeeServiceTest**: Métodos testados incluem `save`, `findAll`, `findById`, `update` e `delete`. Abrange casos de sucesso no cadastro e atualização, verificação de existência e duplicidade dentro de departamentos, além da exclusão correta de funcionários com ou sem foto. Localização: `src/test/java/com/api/staff_manager/services/EmployeeServiceTest.java`  
- **UserServiceTest**: Métodos testados incluem `loadUserByUsername`, `save`, `findAll`, `findById`, `findByEmail`, `update` e `delete`. Os testes cobrem criação de usuários com senha codificada, autenticação, buscas por diferentes critérios, atualização com validação de email e exclusão segura. Localização: `src/test/java/com/api/staff_manager/services/UserServiceTest.java`    

Cerca de 60% das classes da camada service estão cobertas por testes unitários, garantindo a robustez das operações e o correto uso das camadas de persistência e mapeamento.

## Estado Atual

Este projeto está em desenvolvimento e atualmente a versão é a 0.0.1.

## Contribuição

Para contribuir com o projeto, siga os passos abaixo:

- Faça um fork do repositório  
- Crie uma branch com a feature:  
  ```bash
  git checkout -b feature/nova-funcionalidade
  ```
- Faça commit das suas alterações e envie ao repositório remoto  
- Abra um pull request para o branch principal do projeto  

Contribuições são muito bem-vindas! Por favor, siga as normas de código e padrões do projeto.

Se precisar de ajuda ou quiser sugerir melhorias, fique à vontade para abrir issues no repositório.

## Observações

- As chaves secretas e configurações de tokens devem ser protegidas e configuradas via variáveis de ambiente.  
- Os tokens JWT possuem tempos de validade diferentes:  
  - O **access token** tem validade de **15 minutos** para minimizar riscos em caso de comprometimento.  
  - O **refresh token** possui validade maior, de **24 horas**, e é utilizado para obter novos access tokens sem que o usuário precise se autenticar novamente.  
- Para maior segurança, nos métodos da classe `AuthController` recomenda-se usar `.secure(true)` nos cookies em produção para garantir envio apenas via HTTPS, e ajustar `.sameSite("None")` quando for necessário permitir que os cookies funcionem entre domínios diferentes.
