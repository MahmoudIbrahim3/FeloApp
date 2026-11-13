-- Phase 8: four age bands (2-3, 4-6, 7-9, 10-12) become three (3-5, 6-8, 9-12), Google Play's
-- Families age groups. Apps from before this change drop rows they can't read, so the app and the
-- backend switch together (nothing is released yet).
--
-- Existing rows keep all their content: a band that straddles two new ones maps to both
-- (4-6 -> 3-5 + 6-8, 7-9 -> 6-8 + 9-12). Review the result in Studio. The updated_at triggers fire,
-- so apps download every row again.

create function pg_temp.new_age_bands(old text[]) returns text[]
language sql immutable as $$
  select array_agg(distinct band order by band)
  from unnest(old) as o(value)
  cross join lateral unnest(case o.value
    when '2-3' then array['3-5']
    when '4-6' then array['3-5', '6-8']
    when '7-9' then array['6-8', '9-12']
    when '10-12' then array['9-12']
    else array[o.value]
  end) as band;
$$;

alter table public.collections drop constraint if exists collections_age_bands_check;
alter table public.videos drop constraint if exists videos_age_bands_check;

update public.collections set age_bands = pg_temp.new_age_bands(age_bands);
update public.videos set age_bands = pg_temp.new_age_bands(age_bands);

alter table public.collections add constraint collections_age_bands_check check (
  cardinality(age_bands) > 0 and age_bands <@ array['3-5', '6-8', '9-12']
);
alter table public.videos add constraint videos_age_bands_check check (
  cardinality(age_bands) > 0 and age_bands <@ array['3-5', '6-8', '9-12']
);
