/* ============================================================================
 * API endpoint registry — THE single place every API path is defined.
 *
 * Nothing in the app should hard-code a URL string. Import a path from here:
 *     Api.get(API.master.route.list)
 *     Api.post(API.master.route.status(oid), null, { query: { status: 'N' } })
 *
 * Static paths are plain strings; parameterised paths are functions. Change a
 * route in ONE place and every caller follows. Frozen so nothing mutates it.
 * ==========================================================================*/
(function (window) {
  "use strict";

  const API = {
    auth: {
      login: "/api/auth/login",
      refresh: "/api/auth/refresh",
      logout: "/api/auth/logout",
    },
    me: "/api/me",
    lookups: "/api/lookups",
    ping: "/api/public/ping",
    dashboard: {
      counts: "/api/dashboard/counts",
    },
    settings: {
      get: "/api/settings",
      save: "/api/settings",
    },
    code: {
      empCode: "/api/code/emp-code",                 // POST: reserve next code
      empCodeConfig: "/api/code/emp-code/config",    // GET current + preview, POST to save format
    },
    ai: {
      meta: "/api/ai/meta",           // providers/models + readiness
      settings: "/api/ai/settings",   // GET (admin) / POST save provider+key+model
      chat: "/api/ai/chat",           // POST { messages, model } -> { html }
    },

    i18n: {
      bundle: (lang) => "/api/i18n/bundle" + (lang ? "?lang=" + encodeURIComponent(lang) : ""),
      languages: "/api/i18n/languages",
      getLang: "/api/i18n/lang",
      setLang: (lang) => "/api/i18n/lang?lang=" + encodeURIComponent(lang),
    },

    permissions: {
      my: "/api/permissions/my",   // GET { CODE: true|false } for the current user
    },
    permissionMaster: {
      list: "/api/master/permission-master",               // GET catalog, POST create/update a code
      save: "/api/master/permission-master",
      targets: "/api/master/permission-master/targets",    // GET employees / designations / emp levels
      assignments: "/api/master/permission-master/assignments",  // GET ?targetType=&targetValue=, POST upsert
      deleteAssignment: (oid) => `/api/master/permission-master/assignments/${oid}`,
    },

    menu: {
      get: "/api/menu",
      favorite: (id) => `/api/menu/favorites/${id}`,   // POST to add, DELETE to remove
    },

    menuVisibility: {
      employees: "/api/menu-visibility/employees",
      tree: (target) => "/api/menu-visibility?target=" + encodeURIComponent(target),
      save: "/api/menu-visibility",
      reorder: "/api/menu-visibility/reorder",
      type: "/api/menu-visibility/type",
    },
    menuMaster: {
      base: "/api/menu-master",
      list: "/api/menu-master",
      get: (id) => `/api/menu-master/${id}`,
      create: "/api/menu-master",
      update: (id) => `/api/menu-master/${id}`,
      status: (id) => `/api/menu-master/${id}/status`,
      delete: (id) => `/api/menu-master/${id}`,
    },
    menuAuditLog: {
      list: "/api/menu-audit-log",
    },
    menuCopy: {
      tenants: "/api/menu-copy/tenants",
      tree: (companyCode) => "/api/menu-copy/tree?companyCode=" + encodeURIComponent(companyCode),
      copy: "/api/menu-copy",
    },

    profile: {
      get: "/api/profile",
      save: "/api/profile",
      photo: "/api/profile/photo",
      password: "/api/profile/password",
    },
    company: {
      get: "/api/company",
      save: "/api/company",
      logo: "/api/company/logo",
    },
    branding: {
      get: "/api/branding",
      publicGet: "/api/public/branding",       // no auth
      defaultLogo: "/api/branding/logo",       // POST multipart upload / DELETE clear (super-admin)
      appName: "/api/branding/app-name",       // POST { appName } (super-admin)
      publicImage: "/api/public/branding-image", // GET ?file=  (public, for <img>)
    },

    theme: {
      get: "/api/theme",
      update: "/api/theme",          // PUT
      reset: "/api/theme/reset",     // POST ?mode=light|dark
    },

    loader: {
      get: "/api/loader",             // GET current loader config
      save: "/api/loader",            // POST { type, anim, size, speed, text }
      image: "/api/loader/image",     // POST (multipart) upload, DELETE remove
      publicImage: "/api/public/loader-image",   // GET ?companyCode=&file=  (public, for <img>)
    },

    products: {
      list: "/api/products",
      save: "/api/products",
    },

    master: {
      route: {
        base: "/api/master/route",
        list: "/api/master/route",
        save: "/api/master/route",
        saveMultiple: "/api/master/route/save-multiple",
        get: (oid) => `/api/master/route/${oid}`,
        status: (oid) => `/api/master/route/${oid}/status`,
        template: "/api/master/route/template",
        upload: "/api/master/route/upload",
      },
      area: {
        base: "/api/master/area",
        list: "/api/master/area",
        save: "/api/master/area",
        saveMultiple: "/api/master/area/save-multiple",
        get: (oid) => `/api/master/area/${oid}`,
        status: (oid) => `/api/master/area/${oid}/status`,
        template: "/api/master/area/template",
        upload: "/api/master/area/upload",
      },
      routeArea: {
        base: "/api/master/route-area",
        list: "/api/master/route-area",
        save: "/api/master/route-area",
        status: (oid) => `/api/master/route-area/${oid}/status`,
      },
      clientType: {
        base: "/api/master/client-type",
        list: "/api/master/client-type",
        save: "/api/master/client-type",
        saveMultiple: "/api/master/client-type/save-multiple",
        get: (oid) => `/api/master/client-type/${oid}`,
        status: (oid) => `/api/master/client-type/${oid}/status`,
      },
      visitType: {
        base: "/api/master/visit-type",
        list: "/api/master/visit-type",
        save: "/api/master/visit-type",
        saveMultiple: "/api/master/visit-type/save-multiple",
        get: (oid) => `/api/master/visit-type/${oid}`,
        status: (oid) => `/api/master/visit-type/${oid}/status`,
      },
      activityType: {
        base: "/api/master/activity-type",
        list: "/api/master/activity-type",
        save: "/api/master/activity-type",
        saveMultiple: "/api/master/activity-type/save-multiple",
        get: (oid) => `/api/master/activity-type/${oid}`,
        status: (oid) => `/api/master/activity-type/${oid}/status`,
      },
      client: {
        base: "/api/master/client",
        list: "/api/master/client",
        save: "/api/master/client",
        get: (oid) => `/api/master/client/${oid}`,
        status: (oid) => `/api/master/client/${oid}/status`,
        template: "/api/master/client/template",
        upload: "/api/master/client/upload",
      },
      employee: {
        base: "/api/master/employee",
        list: "/api/master/employee",
        save: "/api/master/employee",
        get: (oid) => `/api/master/employee/${oid}`,
        status: (oid) => `/api/master/employee/${oid}/status`,
      },
      createEmployee: {
        base: "/api/master/create-employee",
        list: "/api/master/create-employee",
        save: "/api/master/create-employee",
        get: (empId) => `/api/master/create-employee/${empId}`,
        status: (empId) => `/api/master/create-employee/${empId}/status`,
        template: "/api/master/create-employee/template",
        upload: "/api/master/create-employee/upload",
      },
      categoryMaster: {
        base: "/api/master/category-master",
        list: "/api/master/category-master",
        save: "/api/master/category-master",
        saveMultiple: "/api/master/category-master/save-multiple",
        get: (oid) => `/api/master/category-master/${oid}`,
        status: (oid) => `/api/master/category-master/${oid}/status`,
      },
      degreeMaster: {
        base: "/api/master/degree-master",
        list: "/api/master/degree-master",
        save: "/api/master/degree-master",
        saveMultiple: "/api/master/degree-master/save-multiple",
        get: (id) => `/api/master/degree-master/${id}`,
        status: (id) => `/api/master/degree-master/${id}/status`,
      },
      itemTypeMaster: {
        base: "/api/master/item-type-master",
        list: "/api/master/item-type-master",
        save: "/api/master/item-type-master",
        saveMultiple: "/api/master/item-type-master/save-multiple",
        get: (id) => `/api/master/item-type-master/${id}`,
        status: (id) => `/api/master/item-type-master/${id}/status`,
      },
      bankMaster: {
        base: "/api/master/bank-master",
        list: "/api/master/bank-master",
        save: "/api/master/bank-master",
        saveMultiple: "/api/master/bank-master/save-multiple",
        get: (id) => `/api/master/bank-master/${id}`,
        status: (id) => `/api/master/bank-master/${id}/status`,
      },
      imageTypeMaster: {
        base: "/api/master/image-type-master",
        list: "/api/master/image-type-master",
        save: "/api/master/image-type-master",
        saveMultiple: "/api/master/image-type-master/save-multiple",
        get: (id) => `/api/master/image-type-master/${id}`,
        status: (id) => `/api/master/image-type-master/${id}/status`,
      },
      meetingTypeMaster: {
        base: "/api/master/meeting-type-master",
        list: "/api/master/meeting-type-master",
        save: "/api/master/meeting-type-master",
        saveMultiple: "/api/master/meeting-type-master/save-multiple",
        get: (id) => `/api/master/meeting-type-master/${id}`,
        status: (id) => `/api/master/meeting-type-master/${id}/status`,
      },
      travelTypeMaster: {
        base: "/api/master/travel-type-master",
        list: "/api/master/travel-type-master",
        save: "/api/master/travel-type-master",
        saveMultiple: "/api/master/travel-type-master/save-multiple",
        get: (id) => `/api/master/travel-type-master/${id}`,
        status: (id) => `/api/master/travel-type-master/${id}/status`,
      },
      sponsorshipTypeMaster: {
        base: "/api/master/sponsorship-type-master",
        list: "/api/master/sponsorship-type-master",
        save: "/api/master/sponsorship-type-master",
        saveMultiple: "/api/master/sponsorship-type-master/save-multiple",
        get: (id) => `/api/master/sponsorship-type-master/${id}`,
        status: (id) => `/api/master/sponsorship-type-master/${id}/status`,
      },
      documentMaster: {
        base: "/api/master/document-master",
        list: "/api/master/document-master",
        save: "/api/master/document-master",
        saveMultiple: "/api/master/document-master/save-multiple",
        get: (id) => `/api/master/document-master/${id}`,
        status: (id) => `/api/master/document-master/${id}/status`,
      },
      specialityMaster: {
        base: "/api/master/speciality-master",
        list: "/api/master/speciality-master",
        save: "/api/master/speciality-master",
        saveMultiple: "/api/master/speciality-master/save-multiple",
        get: (oid) => `/api/master/speciality-master/${oid}`,
        status: (oid) => `/api/master/speciality-master/${oid}/status`,
      },
      country: {
        base: "/api/master/country",
        list: "/api/master/country",
        save: "/api/master/country",
        saveMultiple: "/api/master/country/save-multiple",
        get: (oid) => `/api/master/country/${oid}`,
        status: (oid) => `/api/master/country/${oid}/status`,
      },
      division: {
        base: "/api/master/division",
        list: "/api/master/division",
        save: "/api/master/division",
        saveMultiple: "/api/master/division/save-multiple",
        get: (oid) => `/api/master/division/${oid}`,
        status: (oid) => `/api/master/division/${oid}/status`,
        template: "/api/master/division/template",
        upload: "/api/master/division/upload",
      },
      zone: {
        base: "/api/master/zone",
        list: "/api/master/zone",
        save: "/api/master/zone",
        saveMultiple: "/api/master/zone/save-multiple",
        get: (oid) => `/api/master/zone/${oid}`,
        status: (oid) => `/api/master/zone/${oid}/status`,
        template: "/api/master/zone/template",
        upload: "/api/master/zone/upload",
      },
      state: {
        base: "/api/master/state",
        list: "/api/master/state",
        save: "/api/master/state",
        saveMultiple: "/api/master/state/save-multiple",
        get: (oid) => `/api/master/state/${oid}`,
        status: (oid) => `/api/master/state/${oid}/status`,
        template: "/api/master/state/template",
        upload: "/api/master/state/upload",
      },
      hq: {
        base: "/api/master/hq",
        list: "/api/master/hq",
        save: "/api/master/hq",
        saveMultiple: "/api/master/hq/save-multiple",
        get: (oid) => `/api/master/hq/${oid}`,
        status: (oid) => `/api/master/hq/${oid}/status`,
        template: "/api/master/hq/template",
        upload: "/api/master/hq/upload",
      },
      hqGroupMaster: {
        base: "/api/master/hq-group-master",
        list: "/api/master/hq-group-master",
        save: "/api/master/hq-group-master",
        saveMultiple: "/api/master/hq-group-master/save-multiple",
        get: (id) => `/api/master/hq-group-master/${id}`,
        status: (id) => `/api/master/hq-group-master/${id}/status`,
      },
    },

    report: {
      dcr: "/api/report/dcr",
    },

    reportActivity: {
      list: "/api/report-activity",
      save: "/api/report-activity",
    },
  };

  function deepFreeze(o) {
    Object.getOwnPropertyNames(o).forEach((k) => {
      const v = o[k];
      if (v && typeof v === "object") deepFreeze(v);
    });
    return Object.freeze(o);
  }

  window.API = deepFreeze(API);
})(window);
