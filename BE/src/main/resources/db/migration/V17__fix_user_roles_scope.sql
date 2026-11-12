-- V17: Fix user_roles scope check to allow PLATFORM and other scopes, and auto-sync scope via trigger

ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS chk_user_roles_role_scope;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS fk_user_roles_role_scoped;

CREATE OR REPLACE FUNCTION travel_v2.trg_set_user_role_scope()
RETURNS TRIGGER AS $$
BEGIN
    SELECT scope INTO NEW.role_scope FROM travel_v2.roles WHERE id = NEW.role_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_roles_scope ON user_roles;
CREATE TRIGGER trg_user_roles_scope
BEFORE INSERT OR UPDATE ON user_roles
FOR EACH ROW EXECUTE FUNCTION travel_v2.trg_set_user_role_scope();

ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role_scoped
    FOREIGN KEY (role_id, role_scope) REFERENCES roles(id, scope) ON DELETE RESTRICT;
