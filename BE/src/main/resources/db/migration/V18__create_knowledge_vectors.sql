-- V18: Store product knowledge chunks and their embeddings.
-- Requires the pgvector extension to be installed in the PostgreSQL image.

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

CREATE TABLE knowledge_vectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_document_id UUID NOT NULL
        REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding public.vector(768) NOT NULL
);

CREATE INDEX idx_knowledge_vectors_document_id
    ON knowledge_vectors (knowledge_document_id);

-- Remove stale chunks when the source document changes.
-- ChatService will lazily create new chunks and embeddings on the next request.
CREATE OR REPLACE FUNCTION invalidate_knowledge_vectors_after_document_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.content IS DISTINCT FROM NEW.content THEN
        DELETE FROM knowledge_vectors
        WHERE knowledge_document_id = NEW.id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_invalidate_knowledge_vectors_after_document_update
AFTER UPDATE OF content ON knowledge_documents
FOR EACH ROW
EXECUTE FUNCTION invalidate_knowledge_vectors_after_document_update();