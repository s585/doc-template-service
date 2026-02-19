-- liquibase formatted sql

-- changeset codex:020_generated_document
CREATE TABLE IF NOT EXISTS t_generated_document (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id        UUID NOT NULL,

  template_id      UUID NOT NULL,

  entity_id        UUID NOT NULL,
  object_id        UUID NOT NULL,

  request_id       UUID NOT NULL,

  created_by       UUID,
  updated_by       UUID,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_t_generated_document_tenant_entity
  ON t_generated_document (tenant_id, entity_id, object_id);

CREATE INDEX IF NOT EXISTS idx_t_generated_document_created_at
  ON t_generated_document (created_at);

ALTER TABLE t_generated_document ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_generated_document_tenant ON t_generated_document;
CREATE POLICY rls_t_generated_document_tenant
ON t_generated_document
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:020_generated_document_comments runOnChange:true
COMMENT ON TABLE t_generated_document IS
'Written read-model entity for UI: a single user request/result container. Not a job table. RLS by tenant_id.';

COMMENT ON COLUMN t_generated_document.request_id IS
'Idempotency key for generation request (unique per tenant).';
