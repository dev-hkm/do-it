package at.co.schwaerzler.maximilian.doit.util

import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import at.co.schwaerzler.maximilian.doit.R

enum class AppFont(
    @param:StringRes val labelRes: Int,
    @param:FontRes val fontRes: Int
) {
    NUNITO(R.string.font_nunito, R.font.nunito),
    INTER(R.string.font_inter, R.font.inter),
    PLUS_JAKARTA_SANS(R.string.font_plus_jakarta_sans, R.font.plus_jakarta_sans),
    BE_VIETNAM_PRO(R.string.font_be_vietnam_pro, R.font.be_vietnam_pro),
    OUTFIT(R.string.font_outfit, R.font.outfit),
    POPPINS(R.string.font_poppins, R.font.poppins),
    LEXEND(R.string.font_lexend, R.font.lexend),
    MONTSERRAT(R.string.font_montserrat, R.font.montserrat),
    SPACE_GROTESK(R.string.font_space_grotesk, R.font.space_grotesk),
    JETBRAINS_MONO(R.string.font_jetbrains_mono, R.font.jetbrains_mono),
    PLAYFAIR_DISPLAY(R.string.font_playfair_display, R.font.playfair_display),
    COMFORTAA(R.string.font_comfortaa, R.font.comfortaa),
    UBUNTU(R.string.font_ubuntu, R.font.ubuntu),
    SPACE_MONO(R.string.font_space_mono, R.font.space_mono),
    EB_GARAMOND(R.string.font_eb_garamond, R.font.eb_garamond),
    ROBOTO_SLAB(R.string.font_roboto_slab, R.font.roboto_slab),
    PATRICK_HAND(R.string.font_patrick_hand, R.font.patrick_hand),
    EDU_SA_HAND(R.string.font_edu_sa_hand, R.font.edu_sa_hand);

    val family: FontFamily
        get() = FontFamily(Font(fontRes, FontWeight.Normal))
}
