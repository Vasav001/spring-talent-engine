CREATE TABLE jobs (
    id          UUID                     PRIMARY KEY,
    tenant_id   UUID                     NOT NULL REFERENCES tenants (id),
    title       TEXT                     NOT NULL,
    description TEXT                     NOT NULL,
    status      TEXT                     NOT NULL,
    created_by  TEXT                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX jobs_tenant_created_idx ON jobs (tenant_id, created_at);

CREATE TABLE job_requirements (
    id        UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    job_id    UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    skill     TEXT NOT NULL,
    type      TEXT NOT NULL,
    weight    INT  NOT NULL,
    min_years INT
);

CREATE INDEX job_requirements_job_idx ON job_requirements (job_id);

ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_requirements ENABLE ROW LEVEL SECURITY;

CREATE POLICY jobs_tenant ON jobs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY job_requirements_tenant ON job_requirements
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE TABLE skill_synonyms (
    alias     TEXT PRIMARY KEY,
    canonical TEXT NOT NULL
);

INSERT INTO skill_synonyms (alias, canonical) VALUES
    ('java', 'Java'),
    ('spring', 'Spring'),
    ('spring boot', 'Spring Boot'),
    ('springboot', 'Spring Boot'),
    ('spring-boot', 'Spring Boot'),
    ('spring framework', 'Spring'),
    ('hibernate', 'Hibernate'),
    ('jpa', 'JPA'),
    ('kotlin', 'Kotlin'),
    ('python', 'Python'),
    ('go', 'Go'),
    ('golang', 'Go'),
    ('c#', 'C#'),
    ('csharp', 'C#'),
    ('.net', '.NET'),
    ('dotnet', '.NET'),
    ('javascript', 'JavaScript'),
    ('js', 'JavaScript'),
    ('typescript', 'TypeScript'),
    ('ts', 'TypeScript'),
    ('react', 'React'),
    ('reactjs', 'React'),
    ('react.js', 'React'),
    ('angular', 'Angular'),
    ('angularjs', 'Angular'),
    ('vue', 'Vue.js'),
    ('vuejs', 'Vue.js'),
    ('vue.js', 'Vue.js'),
    ('node', 'Node.js'),
    ('nodejs', 'Node.js'),
    ('node.js', 'Node.js'),
    ('sql', 'SQL'),
    ('postgres', 'PostgreSQL'),
    ('postgresql', 'PostgreSQL'),
    ('mysql', 'MySQL'),
    ('mongodb', 'MongoDB'),
    ('mongo', 'MongoDB'),
    ('redis', 'Redis'),
    ('kafka', 'Kafka'),
    ('apache kafka', 'Kafka'),
    ('rabbitmq', 'RabbitMQ'),
    ('docker', 'Docker'),
    ('kubernetes', 'Kubernetes'),
    ('k8s', 'Kubernetes'),
    ('aws', 'AWS'),
    ('amazon web services', 'AWS'),
    ('gcp', 'GCP'),
    ('google cloud', 'GCP'),
    ('azure', 'Azure'),
    ('microsoft azure', 'Azure'),
    ('terraform', 'Terraform'),
    ('ci/cd', 'CI/CD'),
    ('cicd', 'CI/CD'),
    ('ci cd', 'CI/CD'),
    ('git', 'Git'),
    ('rest', 'REST APIs'),
    ('rest api', 'REST APIs'),
    ('rest apis', 'REST APIs'),
    ('restful', 'REST APIs'),
    ('restful apis', 'REST APIs'),
    ('graphql', 'GraphQL'),
    ('microservices', 'Microservices'),
    ('microservice', 'Microservices'),
    ('machine learning', 'Machine Learning'),
    ('ml', 'Machine Learning'),
    ('linux', 'Linux');
