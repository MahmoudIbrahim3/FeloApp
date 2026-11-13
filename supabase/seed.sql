-- The six launch collections (same titles, colours and marks as the app's design system).
-- Applied automatically by `supabase db reset` locally. For the hosted project, run this once in
-- the SQL Editor (see README.md). Safe to run again: existing slugs are left as they are.

insert into public.collections
  (slug, title_ar, cover_bg, cover_fg, cover_glyph, cover_icon, age_bands, sort_order, is_published)
values
  ('letters',    'الحروف',       '#FFE6B0', '#8A5A00', 'أ ب', null,    array['3-5'],                 1, true),
  ('numbers',    'الأرقام',      '#D6E8FF', '#1A4FB5', '١٢٣', null,    array['3-5', '6-8'],          2, true),
  ('quran-duas', 'قرآن وأدعية',  '#D9F2E4', '#1F7A4A', null,  'book',  array['3-5', '6-8', '9-12'],  3, true),
  ('manners',    'أخلاق وسلوك',  '#FFDCD6', '#B23A26', null,  'heart', array['3-5', '6-8'],          4, true),
  ('science',    'علوم',         '#E6DEFF', '#5B3DC4', null,  'leaf',  array['6-8', '9-12'],         5, true),
  ('stories',    'حواديت',       '#FFE3C7', '#A3530E', null,  'moon',  array['3-5', '6-8', '9-12'],  6, true)
on conflict (slug) do nothing;
