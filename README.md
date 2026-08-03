# ♿ AccessMap

O AccessMap é uma plataforma composta por um aplicativo mobile e um portal web que permite à comunidade mapear colaborativamente pontos de acessibilidade e barreiras urbanas. 

O objetivo é fornecer informações reais e atualizadas sobre a acessibilidade dos espaços urbanos, garantindo maior autonomia para pessoas com deficiência ou mobilidade reduzida, além de gerar dados abertos para o embasamento de políticas públicas de inclusão.

## 🚀 Funcionalidades Principais
* **Mapeamento Colaborativo:** Reporte de barreiras (ex: calçadas danificadas, ausência de rampas) e pontos de acessibilidade, com anexo de fotos e localização exata.
* **Validação Comunitária:** Sistema de confirmação coletiva para garantir a confiabilidade das informações.
* **Filtros Personalizados:** Filtragem de resultados por tipo de necessidade (mobilidade reduzida, deficiência visual, auditiva, etc.).
* **Rotas Acessíveis:** Traçado de rotas entre pontos considerando estritamente caminhos adaptados.

## 🛠️ Tecnologias e Arquitetura

O projeto adota uma arquitetura RESTful com separação clara de responsabilidades:

**Back-end**
* Java 17+ com Spring Boot
* PostgreSQL + PostGIS (via Hibernate Spatial) para dados geoespaciais
* Spring Security + JWT para autenticação
* Swagger/OpenAPI para documentação
* JUnit e Mockito para testes automatizados

**Front-end (Web e Mobile)**
* React com TypeScript e Tailwind CSS (Portal Web)
* Leaflet.js (Mapas interativos)
* React Native com Expo (Aplicativo Mobile)
* Jest e Cypress para testes

**DevOps & CI/CD**
* Git & GitHub Actions
* Deploy contínuo na plataforma Railway

## 👥 Equipe Desenvolvedora
* João Victor Martins
* Gustavo Borges Hertz
* Gabriel Maurity
* Gabriel Rezende

## 🚦 Próximos Passos (Em Desenvolvimento)
1. Estruturação do repositório base.
2. Definição e documentação dos contratos de API (Endpoints).
3. Configuração do banco de dados geoespacial.
