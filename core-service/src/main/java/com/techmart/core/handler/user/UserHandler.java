package com.techmart.core.handler.user;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.user.AddAddressCommand;
import com.techmart.core.bus.command.user.CreateStaffCommand;
import com.techmart.core.bus.command.user.DeleteAddressCommand;
import com.techmart.core.bus.command.user.DeleteStaffCommand;
import com.techmart.core.bus.command.user.SetDefaultAddressCommand;
import com.techmart.core.bus.command.user.UpdateAddressCommand;
import com.techmart.core.bus.command.user.UpdateProfileCommand;
import com.techmart.core.bus.query.user.GetCustomerDetailQuery;
import com.techmart.core.bus.query.user.GetCustomerListQuery;
import com.techmart.core.bus.query.user.GetStaffListQuery;
import com.techmart.core.bus.query.user.GetUserAddressesQuery;
import com.techmart.core.bus.query.user.GetUserProfileQuery;
import com.techmart.core.dto.response.user.AddressResponse;
import com.techmart.core.dto.response.user.CustomerDetailResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.core.service.command.user.UserCommandService;
import com.techmart.core.service.query.user.UserQueryService;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.stereotype.Component;

import java.util.List;

public class UserHandler {

    @Component
    public static class UpdateProfileCommandHandler implements CommandHandler<UpdateProfileCommand, UserProfileResponse> {
        private final UserCommandService userCommandService;

        public UpdateProfileCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public UserProfileResponse handle(UpdateProfileCommand command) {
            return userCommandService.updateProfile(command.userId(), command.request());
        }
    }

    @Component
    public static class AddAddressCommandHandler implements CommandHandler<AddAddressCommand, AddressResponse> {
        private final UserCommandService userCommandService;

        public AddAddressCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public AddressResponse handle(AddAddressCommand command) {
            return userCommandService.addAddress(command.userId(), command.request());
        }
    }

    @Component
    public static class UpdateAddressCommandHandler implements CommandHandler<UpdateAddressCommand, AddressResponse> {
        private final UserCommandService userCommandService;

        public UpdateAddressCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public AddressResponse handle(UpdateAddressCommand command) {
            return userCommandService.updateAddress(command.userId(), command.addressId(), command.request());
        }
    }

    @Component
    public static class DeleteAddressCommandHandler implements CommandHandler<DeleteAddressCommand, Void> {
        private final UserCommandService userCommandService;

        public DeleteAddressCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public Void handle(DeleteAddressCommand command) {
            userCommandService.deleteAddress(command.userId(), command.addressId());
            return null;
        }
    }

    @Component
    public static class SetDefaultAddressCommandHandler implements CommandHandler<SetDefaultAddressCommand, AddressResponse> {
        private final UserCommandService userCommandService;

        public SetDefaultAddressCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public AddressResponse handle(SetDefaultAddressCommand command) {
            return userCommandService.setDefaultAddress(command.userId(), command.addressId());
        }
    }

    @Component
    public static class CreateStaffCommandHandler implements CommandHandler<CreateStaffCommand, UserProfileResponse> {
        private final UserCommandService userCommandService;

        public CreateStaffCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public UserProfileResponse handle(CreateStaffCommand command) {
            return userCommandService.createStaff(command.request(), command.createdBy());
        }
    }

    @Component
    public static class DeleteStaffCommandHandler implements CommandHandler<DeleteStaffCommand, Void> {
        private final UserCommandService userCommandService;

        public DeleteStaffCommandHandler(UserCommandService userCommandService) {
            this.userCommandService = userCommandService;
        }

        @Override
        public Void handle(DeleteStaffCommand command) {
            userCommandService.deleteStaff(command.staffId());
            return null;
        }
    }

    @Component
    public static class GetUserProfileQueryHandler implements QueryHandler<GetUserProfileQuery, UserProfileResponse> {
        private final UserQueryService userQueryService;

        public GetUserProfileQueryHandler(UserQueryService userQueryService) {
            this.userQueryService = userQueryService;
        }

        @Override
        public UserProfileResponse handle(GetUserProfileQuery query) {
            return userQueryService.getProfile(query.userId());
        }
    }

    @Component
    public static class GetUserAddressesQueryHandler implements QueryHandler<GetUserAddressesQuery, List<AddressResponse>> {
        private final UserQueryService userQueryService;

        public GetUserAddressesQueryHandler(UserQueryService userQueryService) {
            this.userQueryService = userQueryService;
        }

        @Override
        public List<AddressResponse> handle(GetUserAddressesQuery query) {
            return userQueryService.getAddresses(query.userId());
        }
    }

    @Component
    public static class GetStaffListQueryHandler implements QueryHandler<GetStaffListQuery, PagedResponse<UserProfileResponse>> {
        private final UserQueryService userQueryService;

        public GetStaffListQueryHandler(UserQueryService userQueryService) {
            this.userQueryService = userQueryService;
        }

        @Override
        public PagedResponse<UserProfileResponse> handle(GetStaffListQuery query) {
            return userQueryService.getStaffList(query.pageable());
        }
    }

    @Component
    public static class GetCustomerListQueryHandler implements QueryHandler<GetCustomerListQuery, PagedResponse<UserProfileResponse>> {
        private final UserQueryService userQueryService;

        public GetCustomerListQueryHandler(UserQueryService userQueryService) {
            this.userQueryService = userQueryService;
        }

        @Override
        public PagedResponse<UserProfileResponse> handle(GetCustomerListQuery query) {
            return userQueryService.getCustomers(query.keyword(), query.pageable());
        }
    }

    @Component
    public static class GetCustomerDetailQueryHandler implements QueryHandler<GetCustomerDetailQuery, CustomerDetailResponse> {
        private final UserQueryService userQueryService;

        public GetCustomerDetailQueryHandler(UserQueryService userQueryService) {
            this.userQueryService = userQueryService;
        }

        @Override
        public CustomerDetailResponse handle(GetCustomerDetailQuery query) {
            return userQueryService.getCustomerDetail(query.customerId());
        }
    }
}
