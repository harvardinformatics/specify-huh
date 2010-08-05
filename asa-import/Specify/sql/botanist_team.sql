select bt.team_id,
       bt.botanist_id,
       bt.ordinal

from botanist_team bt

order by bt.team_id, bt.ordinal, bt.id