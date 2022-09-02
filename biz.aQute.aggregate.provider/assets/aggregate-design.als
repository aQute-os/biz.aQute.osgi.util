
let ℕ = { n:Int | n >= 0}

sig Type {}
sig Bundle {}
sig ServiceRegistration {}
sig ServiceTracker {}

sig ActualTypeFactory {
	registration        : lone ServiceRegistration,
    tracker             : Bundle -> ServiceTracker,
}

sig AggregateState {
	var bundles			: set TrackedBundle,
	var services		: set TrackedService,
}

sig ServiceInfo {
	serviceType		: Type,
	promises		: ℕ,
    actual          : set Type
}
sig TrackedBundle {
    bundle          : Bundle,
	infos 			: set ServiceInfo
}

sig TrackedService {
	serviceType		: Type,
    adjust        	: ℕ,
    override       	: lone ℕ,

    var discovered  : ℕ,
    var promised    : ℕ,
    var actualTypes : set ActualType,
    var bundleInfos : set BundleInfo,
}
sig BundleInfo {
    bundle              : Bundle,
	var max             : ℕ,
    var actual          : ℕ,
}
sig ActualType {
	actualType	   	    : Type,
	localOverride	    : lone ℕ,

	var clients			: ℕ,
	var reg				: lone ActualTypeFactory
}
