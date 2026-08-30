package org.sahara.app.help

enum class HelpResourceType {
    POLICE,
    HELPLINE,
    NGO,
    LEGAL_AID
}

data class HelpContact(
    val id: String,
    val name: String,
    val type: HelpResourceType,
    val phone: String,
    val city: String,
    val description: String
)

object OfflineHelpDirectory {

    private val bundledContacts = listOf(
        HelpContact(
            id = "mum_police_control",
            name = "Mumbai Police Control Room",
            type = HelpResourceType.POLICE,
            phone = "100",
            city = "Mumbai",
            description = "Main police emergency response control room for Mumbai metropolitan area."
        ),
        HelpContact(
            id = "mum_women_helpline",
            name = "Mumbai Women Helpline",
            type = HelpResourceType.HELPLINE,
            phone = "103",
            city = "Mumbai",
            description = "24/7 dedicated emergency helpline for women in distress across Mumbai."
        ),
        HelpContact(
            id = "national_emergency",
            name = "National Emergency Number",
            type = HelpResourceType.POLICE,
            phone = "112",
            city = "All Maharashtra",
            description = "Single emergency response support system (ERSS) across India."
        ),
        HelpContact(
            id = "majlis_legal_ngo",
            name = "Majlis Legal Centre",
            type = HelpResourceType.LEGAL_AID,
            phone = "+91-22-26661252",
            city = "Mumbai",
            description = "Legal aid, rights advocacy, and support for women."
        ),
        HelpContact(
            id = "pune_police_women_cell",
            name = "Pune Police Women's Cell",
            type = HelpResourceType.POLICE,
            phone = "1091",
            city = "Pune",
            description = "Dedicated women's protection cell in Pune."
        )
    )

    fun getContactsForCity(city: String): List<HelpContact> {
        return bundledContacts.filter { it.city.equals(city, ignoreCase = true) || it.city.equals("All Maharashtra", ignoreCase = true) }
    }

    fun getAllContacts(): List<HelpContact> = bundledContacts
}
