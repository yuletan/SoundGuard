-- Update beneficiary_settings to default auto_approve_camera_requests to false.
-- Beneficiary privacy is preserved beforehand; auto-approval must be an explicit opt-in.

alter table public.beneficiary_settings
    alter column auto_approve_camera_requests set default false;
