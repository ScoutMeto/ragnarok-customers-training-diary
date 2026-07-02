-- ScoutMeto kolo 7: členství. Admin u USER účtu eviduje buď počet zbývajících vstupů,
-- nebo datum konce členství (obojí nullable). Když vstupy klesnou na 0/minus nebo datum
-- projde, pole se v UI (dashboard + admin accounts) zbarví červeně.

ALTER TABLE account
    ADD COLUMN membership_entries SMALLINT,
    ADD COLUMN membership_until   DATE;
