-- Phase 8: every video is free. YouTube Developer Policies III.F.3.a forbid charging to watch
-- content in an embedded YouTube player, so no collection is premium any more. The subscription
-- sells parent features only (more child profiles, the weekly activity report), which live in the
-- app, not in the catalog.

alter table public.collections drop column is_premium;
